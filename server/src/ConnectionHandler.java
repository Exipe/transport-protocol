
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.CRC32;

/**
 * Lab 3 - Server
 *
 * @author Ludwig Johansson (ljn18012), Johan Karlsson (jkn18010)
 *
 * Handles everything related to connections.
 * Opening, responding to data, closing.
 */
public class ConnectionHandler {

    private static final int INCOMING_WINDOW_SIZE = 10;

    private final DatagramSocket socket;

    //The list of connections that are not yet established. I.e. we're waiting for a SYN_ACK.
    private final Map<Integer, Connection> connections = new HashMap<>();

    //The list of connections that have been established.
    private final Map<Integer, EstablishedConnection> establishedConnections = new HashMap<>();

    private int connectionCounter = 0; //used to count ID

    public ConnectionHandler(DatagramSocket socket) {
        this.socket = socket;
    }

    /**
     * Sends the array of data, including the crc.
     *
     * @param address - the address to send it to
     * @param port - the port to send it to
     * @param data - the data to send
     */
    public void send(InetAddress address, int port, byte[] data) {
        var crc = new CRC32();
        crc.update(data);

        var output = ByteBuffer.allocate(8 + data.length); //buffer needs to contain all data in the array and 8 additional bytes for the crc
        var controlValue = crc.getValue();
        output.putLong(controlValue);
        output.put(data);

        byte[] buffer = output.array();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);
        try {
            socket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void handleSyn(InetAddress address, int port) {
        int id = connectionCounter++;
        System.out.println("SYN received, assigned id: " + id);

        var c = new Connection(this, address, port, id, INCOMING_WINDOW_SIZE);
        connections.put(id, c);
        c.start(); //start sending the SYN_ACK on a new thread, as not to block the entire program
    }

    public void handleSynAck(int id) {
        System.out.println("SYN_ACK received from Connection " + id);

        var c = connections.remove(id);
        if(c == null) {
            System.out.println("Connection " + id + " does not exist");
            return;
        }

        c.stop();

        var ec = new EstablishedConnection(this, id, c.address(), c.port(), c.incomingWindowSize());
        establishedConnections.put(id, ec);
    }

    private void sendAck(EstablishedConnection connection, int packetId) {
        var output = ByteBuffer.allocate(8);
        output.putInt(PacketType.ACK.ordinal());
        output.putInt(packetId);

        send(connection.address(), connection.port(), output.array());
    }

    public void handleData(int id, IncomingPacket packet) {
        System.out.println("Received packet " + packet.getId() + " from Connection " + id);

        var ec = establishedConnections.get(id);
        if(ec == null) {
            System.out.println("Established connection does not exist " + id);
            return;
        }

        ec.addPacket(packet);

        System.out.println("Sending ACK on packet " + packet.getId() + " to Connection " + id);
        sendAck(ec, packet.getId());
    }

    public void handleClose(int id) {
        System.out.println("CLOSE received from Connection " + id);

        var ec = establishedConnections.get(id);
        if(ec == null) {
            System.out.println("Established connection does not exist " + id);
            return;
        }

        ec.close();
    }

    public void handleCloseAck(int id) {
        System.out.println("CLOSE_ACK received from Connection " + id);

        var ec = establishedConnections.get(id);
        if(ec == null) {
            System.out.println("Established connection does not exist " + id);
            return;
        }

        ec.confirmClose();
    }

    public void remove(int id) {
        establishedConnections.remove(id);
    }

}
