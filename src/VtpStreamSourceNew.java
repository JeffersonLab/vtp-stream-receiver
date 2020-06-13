import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;


public class VtpStreamSourceNew {
    private String hostname = "clondaq3.jlab.org";
    private int port = 5007;
    private int i;

    private void readSocket() {
        try {
            SocketChannel sc = SocketChannel.open();
            sc.connect(new InetSocketAddress(hostname, port));
            ByteBuffer bb = ByteBuffer.allocate(128 * 2048 * 4);
            while (true) {
                System.out.println("event "+i++);
                int bytesRead = sc.read(bb);
                System.out.println(bytesRead);
                Thread.sleep(1000);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
//                String hex = javax.xml.bind.DatatypeConverter.printHexBinary(bb.array());

    public static void main(String[] args) {
        VtpStreamSourceNew ss = new VtpStreamSourceNew();
        ss.readSocket();

    }
}
