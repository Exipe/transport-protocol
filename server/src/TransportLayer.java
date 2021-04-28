
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;

/**
 * Lab 3 - Server
 *
 * @author Ludwig Johansson (ljn18012), Johan Karlsson (jkn18010)
 *
 * Opens a socket and starts listening to packets.
 * Identifies incoming packets and forwards them to the connection handler.
 */
public class TransportLayer implements Runnable {

    private static final boolean SIMULATE_ERROR = false; //Whether or not we should manually produce errors that can occur in the network

    private final DatagramSocket socket;
    private final ConnectionHandler connectionHandler;

    public TransportLayer(int port) throws SocketException {
        this.socket = new DatagramSocket(port);
        this.connectionHandler = new ConnectionHandler(socket);
    }

    private void handlePacket(InetAddress address, int port, PacketType pktType, ByteBuffer input) {
        switch(pktType) {
            case SYN:
                connectionHandler.handleSyn(address, port);
                break;
            case SYN_ACK:
                connectionHandler.handleSynAck(input.getInt());
                break;
            case DATA:
                int connectionId = input.getInt();
                int packetId = input.getInt();

                var data = new byte[input.limit() - input.position()];
                input.get(data);
                connectionHandler.handleData(connectionId, new IncomingPacket(packetId, data));
                break;
            case CLOSE:
                connectionHandler.handleClose(input.getInt());
                break;
            case CLOSE_ACK:
                connectionHandler.handleCloseAck(input.getInt());
                break;
        }
    }

    private void tick() {
        var buf = new byte[256];
        var packet = new DatagramPacket(buf, buf.length);
        try {
            socket.receive(packet);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        if(SIMULATE_ERROR) {
            if(Math.random() > 0.85) {
                System.out.println("Simulating packet loss");
                return;
            }

            //Every byte in the packet has a chance to be corrupted
            for (int i = 0; i < packet.getLength(); i++) {
                if (Math.random() < 0.01) {
                    System.out.println("Simulating corrupted data, index " + i);
                    buf[i] = (byte) Math.floor(Math.random() * 256);
                }
            }
        }

        var input = ByteBuffer.wrap(buf, 0, packet.getLength());

        //The first 8 bytes (long) is the crc, check if this value applies to the data that comes after
        var crc = input.getLong();
        var checksum = new CRC32();
        checksum.update(buf, 8, packet.getLength() - 8);

        if(crc != checksum.getValue()) {
            System.out.println("Invalid CRC");
            return;
        }

        var pktType = PacketType.values()[input.getInt()]; //Give a name to the packet type, easier code

        handlePacket(packet.getAddress(), packet.getPort(), pktType, input);
    }

    @Override
    public void run() {
        while(true) {
            tick();
        }
    }

}
