import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * Lab 3 - Client
 *
 * @author Ludwig Johansson (ljn18012), Johan Karlsson (jkn18010)
 *
 * Represents an outgoing packet
 */
public class OutgoingPacket {

    private final int id;
    private boolean acked = false;

    public OutgoingPacket(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }

    public void ack() {
        acked = true;
    }

    public boolean isAcked() {
        return acked;
    }

}
