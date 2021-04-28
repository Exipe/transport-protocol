
import java.net.InetAddress;
import java.nio.ByteBuffer;

/**
 * Lab 3 - Server
 *
 * @author Ludwig Johansson (ljn18012), Johan Karlsson (jkn18010)
 *
 * Represents a connection, that has not yet been established.
 * Handles sending the SYN_ACK.
 */
public class Connection {

    private static final int CONNECT_ATTEMPTS = 10;

    private final Thread t;

    private final ConnectionHandler handler;

    private final InetAddress address;
    private final int port;

    private final int id;
    private final int incomingWindowSize;

    private int attempts = 0;

    public Connection(ConnectionHandler handler, InetAddress address, int port, int id, int incomingWindowSize) {
        this.handler = handler;
        this.address = address;
        this.port = port;
        this.id = id;
        this.incomingWindowSize = incomingWindowSize;

        this.t = new Thread(() -> {
            while(attempts < CONNECT_ATTEMPTS) {
                System.out.println("Sending SYN_ACK to Connection " + id + " Attempt " + attempts);

                this.sendSynAck();
                attempts++;

                try {
                    Thread.sleep(2_000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
    }

    private void sendSynAck() {
        var output = ByteBuffer.allocate(12);
        output.putInt(PacketType.SYN_ACK.ordinal());
        output.putInt(this.id);
        output.putInt(this.incomingWindowSize);

        handler.send(this.address, this.port, output.array());
    }

    public InetAddress address() {
        return address;
    }

    public int port() {
        return port;
    }

    public int id() {
        return id;
    }

    public int incomingWindowSize() {
        return incomingWindowSize;
    }

    public void start() {
        t.start();
    }

    public void stop() {
        t.interrupt();
    }

}
