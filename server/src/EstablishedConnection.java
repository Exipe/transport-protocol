
import java.net.InetAddress;
import java.nio.ByteBuffer;

/**
 * Lab 3 - Server
 *
 * @author Ludwig Johansson (ljn18012), Johan Karlsson (jkn18010)
 *
 * Represents a connection, that has been established.
 * Sets up a sliding window for incoming data, and handles data that goes through the sliding window.
 * Handles closing down the connection.
 */
public class EstablishedConnection {

    private static final int CLOSE_ATTEMPTS = 10;

    private int attempts = 0;
    private String inputString = "";
    private int stringLength = 0;

    private final ConnectionHandler handler;
    private final int id;

    private final InetAddress address;
    private final int port;

    private final IncomingWindow incomingWindow;

    private final Thread closeThread;

    public EstablishedConnection(ConnectionHandler handler, int id, InetAddress address, int port, int incomingWindowSize) {
        this.handler = handler;
        this.id = id;
        this.address = address;
        this.port = port;
        this.incomingWindow = new IncomingWindow(incomingWindowSize, this::receivePacket);
        this.closeThread = new Thread(() -> {
            while(this.attempts < CLOSE_ATTEMPTS) {
                System.out.println("Sending CLOSE to " + id + " Attempt " + attempts);

                this.sendClose();
                this.attempts++;

                try {
                    Thread.sleep(2_000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
    }

    public InetAddress address() {
        return address;
    }

    public int port() {
        return port;
    }

    public void addPacket(IncomingPacket packet) {
        incomingWindow.addPacket(packet);
    }

    private void receivePacket(IncomingPacket packet) {
        System.out.print("Processing packet " + packet.getId() + " ");
        var input = ByteBuffer.wrap(packet.getData());

        if(stringLength <= 0) {
            stringLength = input.getInt();
            System.out.println("(string length is " + stringLength + " packets)");
            return;
        }

        String s = new String(input.array());
        inputString += s;
        System.out.println("(\"" + s + "\")");
        stringLength--;

        if(stringLength == 0) {
            System.out.println("Connection " + id + " says: " + inputString);
            inputString = "";
        }
    }

    public void close() {
        closeThread.start();
    }

    public void confirmClose() {
        closeThread.interrupt();
        handler.remove(id);
    }

    private void sendClose() {
        ByteBuffer output = ByteBuffer.allocate(4);
        output.putInt(PacketType.CLOSE.ordinal());
        handler.send(this.address, this.port, output.array());
    }

}
