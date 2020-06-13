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

    public VtpListeningServer() {
        timer = new Timer();
        timer.schedule(new PrintRates(), 0, 1000);

        FRAME_TIME = Utility.toUnsignedBigInteger(ft_const);
        ServerSocket serverSocket;
        try {
            serverSocket = new ServerSocket(6000);
            System.out.println("Server is listening on port " + 6000);
            Socket socket = serverSocket.accept();
            System.out.println("VTP client connected");
            InputStream input = socket.getInputStream();
            dataInputStream = new DataInputStream(new BufferedInputStream(input));
            Utility.readLteUnsined32(dataInputStream);
            Utility.readLteUnsined32(dataInputStream);
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
            System.out.println("total_length      = " + Long.toHexString(total_length));
            System.out.println("payload_length    = " + Long.toHexString(payload_length));
            System.out.println("compressed_length = " + Long.toHexString(compressed_length));
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
                System.out.println("event ratre =" + rate
                        + " Hz.  data rate =" + totalData + " kB/s");
                loop = 10;
                rate = 0;
            }
            totalData = 0;
            loop--;
        }
    }

    public static void main(String[] args) {
        VtpListeningServer vtp = new VtpListeningServer();
        while (true) vtp.readVtpFrame();
    }
}

