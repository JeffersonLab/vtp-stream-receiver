package org.jlab.epsci.ersap.services.receivers;

import org.jlab.epsci.ersap.util.Utility;
//import redis.clients.jedis.Jedis;

import java.io.*;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class StreamReceiver {

    private DataInputStream dataInputStream;
    private static BigInteger FRAME_TIME;
    private static final long ft_const = 65536L;
    private static int streamSourcePort = 6000;
    private static boolean isSoftwareStream = false;

    private volatile double totalData;
    private int loop = 10;
    private int rate;

    private long prev_rec_number;
    private long missed_record;

    // redis..
    //    private Jedis dataLake;
    private Map<Long, byte[]> dataLake_hm;

    public StreamReceiver() {

//        try {
//            dataLake_hm = createChronicleMap();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        Timer timer = new Timer();
        timer.schedule(new PrintRates(), 0, 1000);

        // redis..
//        dataLake = new Jedis("localhost");
//        System.out.println("DataLake connection succeeded. ");
//        System.out.println("DataLake ping - "+dataLake.ping());

        FRAME_TIME = Utility.toUnsignedBigInteger(ft_const);
        ServerSocket serverSocket;
        try {
            serverSocket = new ServerSocket(streamSourcePort);
            System.out.println("Server is listening on port " + streamSourcePort);
            Socket socket = serverSocket.accept();
            System.out.println("VTP client connected");
            InputStream input = socket.getInputStream();
            dataInputStream = new DataInputStream(new BufferedInputStream(input));
            dataInputStream.readInt();
            dataInputStream.readInt();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readSoftStream() {
        try {
            int source_id = Integer.reverseBytes(dataInputStream.readInt());
            int total_length = Integer.reverseBytes(dataInputStream.readInt());
            int payload_length = Integer.reverseBytes(dataInputStream.readInt());
            int compressed_length = Integer.reverseBytes(dataInputStream.readInt());
            int magic = Integer.reverseBytes(dataInputStream.readInt());

            int format_version = Integer.reverseBytes(dataInputStream.readInt());
            int flags = Integer.reverseBytes(dataInputStream.readInt());
            long record_number = Utility.llSwap(Long.reverseBytes(dataInputStream.readLong()));
            long ts_sec = Utility.llSwap(Long.reverseBytes(dataInputStream.readLong()));
            long ts_nsec = Utility.llSwap(Long.reverseBytes(dataInputStream.readLong()));


//            System.out.println("source_id         = " + Integer.toHexString(source_id));
//            System.out.println("total_length      = " + total_length);
//            System.out.println("payload_length    = " + payload_length);
//            System.out.println("compressed_length = " + compressed_length);
//            System.out.println("magic             = " + Integer.toHexString(magic));
//            System.out.println("format_version    = " + Integer.toHexString(format_version));
//            System.out.println("record_number     = " + record_number);
//            System.out.println("ts_sec            = " + ts_sec);
//            System.out.println("ts_nsec           = " + ts_nsec);

            missed_record = missed_record + (record_number - (prev_rec_number + 1));
            prev_rec_number = record_number;

            byte[] dataBuffer = new byte[total_length - (13 * 4)];
            dataInputStream.readFully(dataBuffer);

//         redis ...
//            byte[] key =Utility.long2ByteArray(record_number);
//            dataLake.lpush(key, dataBuffer);
//            dataLake.lpop(key);

            totalData = totalData + (double) total_length / 1000.0;
            rate++;

        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void readVtpStream() {
        try {
            int source_id = Integer.reverseBytes(dataInputStream.readInt());
            int total_length = Integer.reverseBytes(dataInputStream.readInt());
            int payload_length = Integer.reverseBytes(dataInputStream.readInt());
            int compressed_length = Integer.reverseBytes(dataInputStream.readInt());
            int magic = Integer.reverseBytes(dataInputStream.readInt());

            // check for magic word = C0DA2019 (below signed int representation
            if (magic == -1059446759) {
                int format_version = Integer.reverseBytes(dataInputStream.readInt());
                int flags = Integer.reverseBytes(dataInputStream.readInt());
                long record_number = Utility.llSwap(Long.reverseBytes(dataInputStream.readLong()));
                long ts_sec = Utility.llSwap(Long.reverseBytes(dataInputStream.readLong()));
                long ts_nsec = Utility.llSwap(Long.reverseBytes(dataInputStream.readLong()));
                long frame_time_ns = record_number * ft_const;

                missed_record = missed_record + (record_number - (prev_rec_number + 1));
                prev_rec_number = record_number;

                byte[] dataBuffer = new byte[payload_length];
                dataInputStream.readFully(dataBuffer);

                // ChronicleMap
                dataLake_hm.put(record_number,dataBuffer);

                // redis...
//                byte[] key =Utility.long2ByteArray(record_number);
//                dataLake.lpush(Long.toString(record_number), "a");
//                dataLake.lpush(key, dataBuffer);
//                dataLake.lpop(key);

//                decodeVtpPayload(dataBuffer);

                totalData = totalData + (double) total_length / 1000.0;
                rate++;

            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void decodeVtpPayload(byte[] payload) {
        if (payload == null) return;
        ByteBuffer bb = ByteBuffer.wrap(payload);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        int[] slot_ind = new int[8];
        int[] slot_len = new int[8];
        long tag = Utility.getUnsignedInt(bb);
        if ((tag & 0x8FFF8000L) == 0x80000000L) {

            for (int jj = 0; jj < 8; jj++) {
                slot_ind[jj] = Utility.getUnsignedShort(bb);
                slot_len[jj] = Utility.getUnsignedShort(bb);
            }
            for (int i = 0; i < 8; i++) {
                if (slot_len[i] > 0) {
                    bb.position(slot_ind[i]);
//                    System.out.println("at entrance "+bb.position()+
//                            " words = "+slot_len[i]/4+
//                            " slot_ind = "+slot_ind[i]+
//                            " slot_len = "+slot_len[i]);
                    for (int j = 0; j < slot_len[i]; j++) {
                        int val = bb.getInt();
                        int type = 0;
                        if ((val & 0x80000000) == 0x80000000) {
                            type = (val >> 15) & 0xFFFF;
                            int rocid = (val >> 8) & 0x007F;
                            int slot = (val) & 0x001F;
                        }
                        if (type == 0x0001) /* FADC hit type */ {
                            int q = (val) & 0x1FFF;
                            int ch = (val >> 13) & 0x000F;
                            int t = ((val >> 17) & 0x3FFF) * 4;
                        }
                    }
                }
            }
        }
    }

    private void decodeSlotData(long[] payload, int slot_ind, long slot_len, BigInteger frame_time_ns) {
        boolean print = true;
        long type = 0, rocid = 0, slot = 0;
        for (int i = slot_ind; i < slot_len; i++) {
            if ((payload[i] & 0x80000000) > 0x0) {
                type = (payload[i] >> 15) & 0xFFFF;
                rocid = (payload[i] >> 8) & 0x007F;
                slot = (payload[i] >> 0) & 0x001F;
            } else if (type == 0x0001) /* FADC hit type */ {
                long q = (payload[i] >> 0) & 0x1FFF;
                long ch = (payload[i] >> 13) & 0x000F;
                long t = ((payload[i] >> 17) & 0x3FFF) * 4;
                BigInteger hit_time = frame_time_ns.add(Utility.toUnsignedBigInteger(t));
/*
                if (print) {
                    System.out.println("---------------------------------");
                    System.out.println("rocId    = " + rocid);
                    System.out.println("slot     = " + slot);
                    System.out.println("q        = " + q);
                    System.out.println("ch       = " + ch);
                    System.out.println("hit_time = " + hit_time);
                    System.out.println("----------------------------------");
                    print = false;
                }
*/
            }
        }
    }

//    private Map<Long, byte[]> createChronicleMap() throws IOException {
//        File file = new File("/temp/dataLake.dat");
//        ChronicleMapBuilder<Long, byte[]> builder = ChronicleMapBuilder.of(Long.class,byte[].class);
//        return builder.createPersistedTo(file);
//    }

    public void receiveSoftStream() {
        while (true) readSoftStream();
    }

    public void receiveVtpStream() {
        while (true) readVtpStream();
    }

    private class PrintRates extends TimerTask {

        @Override
        public void run() {
            if (loop <= 0) {
                System.out.println("event rate =" + rate
                        + " Hz.  data rate =" + totalData + " kB/s." +
                        " missed rate = " + missed_record + " Hz.");
                loop = 10;
            }
            rate = 0;
            missed_record = 0;
            totalData = 0;
            loop--;
        }
    }

    public static void main(String[] args) {
        if (args.length == 3) {
            if (args[0].equals("-p")) {
                streamSourcePort = Integer.parseInt(args[1]);
            } else if (args[0].equals("-s")) {
                isSoftwareStream = true;
            }
            if (args[1].equals("-p")) {
                streamSourcePort = Integer.parseInt(args[2]);
            } else if (args[1].equals("-s")) {
                isSoftwareStream = true;
            }
            if (args[2].equals("-s")) {
                isSoftwareStream = true;
            }
        } else if (args.length == 2) {
            if (args[0].equals("-p")) {
                streamSourcePort = Integer.parseInt(args[1]);
            }
        } else if (args.length == 1) {
            if (args[0].equals("-s")) {
                isSoftwareStream = true;
            }
        }
        StreamReceiver sr = new StreamReceiver();
        if (isSoftwareStream) {
            sr.receiveSoftStream();
        } else {
            sr.receiveVtpStream();
        }
    }
}

