
import java.io.*;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

public class VtpListeningServer {

    private DataInputStream dataInputStream;
    private static BigInteger FRAME_TIME;
    private static final long ft_const = 65536L;

    private volatile double totalData;
    private Timer timer;
    private int loop = 10;
    private int rate;

    private int VTP_PORT = 6000;
    private int SOFT_PORT = 5555;

    private long prev_rec_number;
    private int missed_record;

    public VtpListeningServer() {
        timer = new Timer();
        timer.schedule(new PrintRates(), 0, 1000);

        FRAME_TIME = Utility.toUnsignedBigInteger(ft_const);
        ServerSocket serverSocket;
        try {
            serverSocket = new ServerSocket(VTP_PORT);
            System.out.println("Server is listening on port " + VTP_PORT);
            Socket socket = serverSocket.accept();
            System.out.println("VTP client connected");
            InputStream input = socket.getInputStream();
            dataInputStream = new DataInputStream(new BufferedInputStream(input));

            //            Utility.readLteUnsined32(dataInputStream);
            //            Utility.readLteUnsined32(dataInputStream);
            dataInputStream.readInt();
            dataInputStream.readInt();


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void readVtpFrame() {
        long source_id = Utility.readLteUnsined32(dataInputStream);

        long total_length = Utility.readLteUnsined32(dataInputStream);
        long payload_length = Utility.readLteUnsined32(dataInputStream);
        long compressed_length = Utility.readLteUnsined32(dataInputStream);
        long magic = Utility.readLteUnsined32(dataInputStream);

        if (magic == 3235520537L) {
            long format_version = Utility.readLteUnsined32(dataInputStream);
            long flags = Utility.readLteUnsined32(dataInputStream);
            BigInteger record_number = Utility.readLteUnsignedSwap64(dataInputStream);
            BigInteger ts_sec = Utility.readLteUnsignedSwap64(dataInputStream);
            BigInteger ts_nsec = Utility.readLteUnsignedSwap64(dataInputStream);
            BigInteger frame_time_ns = record_number.multiply(FRAME_TIME);

            totalData = totalData + (double) total_length / 1000;
            rate++;
/*
            System.out.println("source_id         = " + Long.toHexString(source_id));
            System.out.println("total_length      = " + total_length);
            System.out.println("payload_length    = " + payload_length);
            System.out.println("compressed_length = " + compressed_length);
            System.out.println("magic             = " + Long.toHexString(magic));
            System.out.println("format_version    = " + Long.toHexString(format_version));
            System.out.println("flags             = " + Long.toHexString(flags));
            System.out.println("record_number     = " + record_number);
            System.out.println("ts_sec            = " + ts_sec);
            System.out.println("ts_nsec           = " + ts_nsec);
            System.out.println("frame_time_ns     = " + frame_time_ns);
*/
            long[] payload = Utility.readLtPayload(dataInputStream, payload_length);
            decodePayload(payload, frame_time_ns);
        }
    }

    public void readSoftFrame() {
        long source_id = Utility.readLteUnsined32(dataInputStream);
        long total_length = Utility.readLteUnsined32(dataInputStream);
        long payload_length = Utility.readLteUnsined32(dataInputStream);
        long compressed_length = Utility.readLteUnsined32(dataInputStream);
        long magic = Utility.readLteUnsined32(dataInputStream);

        long format_version = Utility.readLteUnsined32(dataInputStream);
        BigInteger record_number = Utility.readLteUnsignedSwap64(dataInputStream);
        BigInteger ts_sec = Utility.readLteUnsignedSwap64(dataInputStream);
        BigInteger ts_nsec = Utility.readLteUnsignedSwap64(dataInputStream);
/*
            System.out.println("source_id         = " + Long.toHexString(source_id));
            System.out.println("total_length      = " + total_length);
            System.out.println("payload_length    = " + payload_length);
            System.out.println("compressed_length = " + compressed_length);
            System.out.println("magic             = " + Long.toHexString(magic));
            System.out.println("format_version    = " + Long.toHexString(format_version));
            System.out.println("record_number     = " + record_number);
            System.out.println("ts_sec            = " + ts_sec);
            System.out.println("ts_nsec           = " + ts_nsec);
*/
        byte[] dataBuffer = new byte[(int) (total_length) - (12 * 4)];
        try {
            dataInputStream.readFully(dataBuffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void readSoftFrame_2() {
        try {
            int source_id = Integer.reverseBytes(dataInputStream.readInt());
            int total_length = Integer.reverseBytes(dataInputStream.readInt());
            int payload_length = Integer.reverseBytes(dataInputStream.readInt());
            int compressed_length = Integer.reverseBytes(dataInputStream.readInt());
            int magic = Integer.reverseBytes(dataInputStream.readInt());

            int format_version = Integer.reverseBytes(dataInputStream.readInt());
            long record_number = Utility.llSwap(Long.reverseBytes(dataInputStream.readLong()));
            long ts_sec = Utility.llSwap(Long.reverseBytes(dataInputStream.readLong()));
            long ts_nsec = Utility.llSwap(Long.reverseBytes(dataInputStream.readLong()));

/*
            System.out.println("source_id         = " + Integer.toHexString(source_id));
            System.out.println("total_length      = " + total_length);
            System.out.println("payload_length    = " + payload_length);
            System.out.println("compressed_length = " + compressed_length);
            System.out.println("magic             = " + Integer.toHexString(magic));
            System.out.println("format_version    = " + Integer.toHexString(format_version));
            System.out.println("record_number     = " + record_number);
            System.out.println("ts_sec            = " + ts_sec);
            System.out.println("ts_nsec           = " + ts_nsec);
*/

            byte[] dataBuffer = new byte[total_length - (12 * 4)];
            dataInputStream.readFully(dataBuffer);

            totalData = totalData + (double) total_length / 1000.0;
            rate++;

        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

    }

    public void readVtpFrame_2() {
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
                long frame_time_ns = record_number*ft_const;

                    if (record_number != (prev_rec_number + 1)) missed_record++;
                prev_rec_number = record_number;

                byte[] dataBuffer = new byte[payload_length];
                dataInputStream.readFully(dataBuffer);

                totalData = totalData + (double) total_length / 1000.0;
                rate++;
/*
            System.out.println("source_id         = " + Long.toHexString(source_id));
            System.out.println("total_length      = " + total_length);
            System.out.println("payload_length    = " + payload_length);
            System.out.println("compressed_length = " + compressed_length);
            System.out.println("magic             = " + Long.toHexString(magic));
            System.out.println("format_version    = " + Long.toHexString(format_version));
            System.out.println("flags             = " + Long.toHexString(flags));
            System.out.println("record_number     = " + record_number);
            System.out.println("ts_sec            = " + ts_sec);
            System.out.println("ts_nsec           = " + ts_nsec);
            System.out.println("frame_time_ns     = " + frame_time_ns);
*/
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void decodePayload(long[] payload, BigInteger frame_time_ns) {
        for (int jj = 1; jj < 9; jj++) /* loop over 8 words, each word corresponds to one FADC slot */ {
            long val = payload[jj];
            long slot_ind = (val >> 0) & 0xFFFF; /*extract index for this FADC data*/
            long slot_len = (val >> 16) & 0xFFFF; /*extract length for this FADC data*/
            /*
            System.out.printf("slot_ind=%d, slot_len=%d\n", slot_ind, slot_len);
             */
            decodeSlotData(payload, (int) slot_ind, slot_len, frame_time_ns);
            /*
            System.out.println();

             */
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


    private class PrintRates extends TimerTask {

        @Override
        public void run() {
            if (loop <= 0) {
                System.out.println("event rate =" + rate
                        + " Hz.  data rate =" + totalData + " kB/s" +
                        " missed "+ missed_record);
                loop = 10;
                rate = 0;
            }
            totalData = 0;
            loop--;
        }
    }

    public static void main(String[] args) {
        VtpListeningServer vtp = new VtpListeningServer();
        while (true) vtp.readVtpFrame_2();
    }
}

