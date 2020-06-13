import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static java.lang.Thread.sleep;

public class VtpStreamSource {
    private static int TIMEOUT = 2000;
    private static String HOST = "clondaq3.jlab.org";
    private int loop;

    private Socket tcpSocket;
    private DataInputStream dataInputStream;

    private byte[] i32 = new byte[4];
    private byte[] sroHeader = new byte[13 * 4];

    private VtpStreamSource(String hostname, int port) {
        tcpSocket = new Socket();
        try {
            tcpSocket.setSoTimeout(TIMEOUT);
            tcpSocket.connect(new InetSocketAddress(hostname, port), TIMEOUT);
            dataInputStream = new DataInputStream(new BufferedInputStream(tcpSocket.getInputStream()));
            System.out.println("connected to " + hostname + " port = " + port);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private long readUnsined32() {
        ByteBuffer bb = null;
        try {
            dataInputStream.readFully(i32);
            bb = ByteBuffer.wrap(i32);
            bb.order(ByteOrder.LITTLE_ENDIAN);
        } catch (IOException e) {
            e.printStackTrace();
        }
        assert bb != null;
        return Utility.getUnsignedInt(bb);
    }

    private void closeStream() {
        try {
            dataInputStream.close();
            tcpSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void decodeT4ns(long x) {

        long t4ns = x & 0x000000000000ffff;
        System.out.println("  ----> t4ns = " + Long.toHexString(t4ns)+" - "+t4ns);
    }
    private void decodeCounter(long x) {

        long frameCounter = (x & 0x000000001fff0000L) >> 16;
        System.out.println("  ----> frameCounter = " + Long.toHexString(frameCounter)+" - "+frameCounter);
    }
    private void decodeDays(long x) {

        long days = (x & 0x0000000000ffc000) >> 14;
        System.out.println("  ----> days = " + Long.toHexString(days));
    }


    private void decodeYears(long x) {

        long years = (x & 0x00000000ff000000) >> 24;
        System.out.println("  ----> years = " + Long.toHexString(years));
    }
    private void decodeSyncBytes(long x) {

        long SuncBytes = (x & 0x00000000ffff0000) >> 16;
        System.out.println("  ----> SuncBytes = " + Long.toHexString(SuncBytes));
    }


    private void test1(int loopParam){
        long w1, w2, w3, w4, w23;
        for (int i = 0; i < loopParam; i++) {

            System.out.println("===================");

            w1 = readUnsined32();
            System.out.println("word_1" + " = " + w1 + " : " + Long.toHexString(w1));
            decodeSyncBytes(w1);

            w2 = readUnsined32();
            System.out.println("word_2" + " = " + w2 + " : " + Long.toHexString(w2));
            decodeT4ns(w2);
            decodeCounter(w2);

            w3 = readUnsined32();
            System.out.println("word_3" + " = " + w3 + " : " + Long.toHexString(w3));
            decodeDays(w3);
            decodeYears(w3);

            w4 = readUnsined32();
            System.out.println("word_4" + " = " + w4 + " : " + Long.toHexString(w4));


            System.out.println("===================");
            System.out.println();

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        VtpStreamSource vss = new VtpStreamSource(HOST, Integer.parseInt(args[0]));
        vss.test1(Integer.parseInt(args[1]));
        vss.closeStream();
    }

}
