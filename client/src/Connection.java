import java.net.*;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;
import java.util.zip.CRC32;

/**
 * Lab 3 - Client
 *
 * @author Johan Karlsson (jkn18010) & Ludwig Johansson (ljn18012)
 *
 * Handles connecting and sending data to the server.
 */
public class Connection implements Runnable {

    private static final boolean SIMULATE_ERROR = false;

    private final Queue<byte[]> dataQueue = new LinkedList<>();

    private DatagramSocket socket;
    private InetAddress address;
    private int port;

    private OutgoingWindow oWindow;
    private IncomingWindow iWindow = new IncomingWindow(20, (x) -> {});

    private Thread synThread;
    private Thread closeThread;

    private int id = -1;
    private boolean shouldRun = true;

    public Connection(InetAddress address, int port) throws SocketException {
        socket = new DatagramSocket();
        this.address = address;
        this.port = port;
    }

    public boolean isConnected() {
        return id >= 0;
    }

    //sends packets if we have data to send from the application.
    public void update() {
        while(oWindow.hasRoom() && !dataQueue.isEmpty()) {
            int packetId = oWindow.nextId();
            byte[] data = dataQueue.remove();

            var output = ByteBuffer.allocate(12 + data.length);
            output.putInt(PacketType.DATA.ordinal());
            output.putInt(id);
            output.putInt(packetId);
            output.put(data, 0, data.length);

            var oPacket = new OutgoingPacket(packetId);
            oWindow.add(oPacket);

            byte[] buf = output.array();

            new Thread(() -> {
                if(SIMULATE_ERROR && Math.random() < 0.5) {
                    System.out.println("Delaying packet " + packetId);

                    try {
                        Thread.sleep((int) (Math.random() * 5_000));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                boolean sent = false;

                while(!oPacket.isAcked()) { //Time-out (we will keep sendingd
                    try {
                        if(!sent) {
                            System.out.println("Sending packet " + packetId);
                            sent = true;
                        }
                        else {
                            System.out.println("Resending packet " + packetId);
                        }
                        sendPacket(buf);
                        Thread.sleep(5000);
                    } catch(Exception e) {
                        e.printStackTrace();
                        break;
                    }
                }
            }).start();
        }
    }

    public void sendData(byte[] data) {
        dataQueue.add(data);
        update();
    }

    //this method will handle everything necessary for closing down the connection.
    public void close() {
        ByteBuffer output = ByteBuffer.allocate(8);
        output.putInt(PacketType.CLOSE.ordinal());
        output.putInt(id);
        byte[] buf = output.array();
       closeThread = new Thread(() -> {
           int attempts = 0;
           while(attempts < 5) {
               try {
                   attempts++;
                   System.out.println("Sending CLOSE packet.");
                   sendPacket(buf);
                   Thread.sleep(2000);
               } catch (InterruptedException e) {
                   break;
               }
           }
           System.out.println("Closing connection.");
           socket.close();
        });
       closeThread.start();
    }

    //general method for sending packets to the server
    private void sendPacket(byte[] buffer) {
        var crc = new CRC32();
        crc.update(buffer);

        ByteBuffer buffer2 = ByteBuffer.allocate(buffer.length + 8);
        buffer2.putLong(crc.getValue());
        buffer2.put(buffer);

        byte[] buffer3 = buffer2.array();
        DatagramPacket packet = new DatagramPacket(buffer3, buffer3.length, address, port);
        try {
            socket.send(packet);
        }
        catch (Exception e)
        {e.printStackTrace();}
    }

    private void sendSYN() {
        ByteBuffer output = ByteBuffer.allocate(8);
        output.putInt(PacketType.SYN.ordinal());
        output.putInt(iWindow.getSize());
        byte[] buf = output.array();

        sendPacket(buf);
    }

    private void sendCloseACK() {
        ByteBuffer output = ByteBuffer.allocate(8);
        output.putInt(PacketType.CLOSE_ACK.ordinal());
        output.putInt(id);
        byte[] buf = output.array();

        sendPacket(buf);
    }

    private void sendSYNACK(int id) throws Exception {
        ByteBuffer output = ByteBuffer.allocate(8);
        output.putInt(PacketType.SYN_ACK.ordinal());
        output.putInt(id);
        byte[] buf = output.array();

        sendPacket(buf);
    }

    @Override
    public void run() {
        //connecting to server
        synThread = new Thread(() -> {
            while(true) {
                try {
                    sendSYN();
                    System.out.println("Sent SYN");
                    Thread.sleep(5_000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        synThread.start();

        //listen for packets
        while(shouldRun) {
            try {
                tick();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void tick() throws Exception {
        var buf = new byte[256];
        DatagramPacket pkt = new DatagramPacket(buf, buf.length);
        socket.receive(pkt);

        var input = ByteBuffer.wrap(buf);

        //this code will give us random errors in order to simulate real scenarios.
        if(SIMULATE_ERROR) {
            if (Math.random() > 0.85) {
                System.out.println("Simulating packet loss");
                return;
            }

            for (int i = 0; i < pkt.getLength(); i++) {
                if (Math.random() < 0.01) {
                    System.out.println("Simulating corrupted data, index " + i);
                    buf[i] = (byte) Math.floor(Math.random() * 256);
                }
            }
        }

        //retrieve crc from the packet
        var crc = input.getLong();

        //calculate crc on packet
        var checksum = new CRC32();
        checksum.update(buf, 8, pkt.getLength() - 8);

        //make sure that both crc values match
        if(crc != checksum.getValue()) {
            System.out.println("Invalid CRC");
            return;
        }

        var pktType = PacketType.values()[input.getInt()];

        switch(pktType) {
            case CLOSE:
                System.out.println("We received CLOSE, lets send a CLOSE_ACK back :)");
                sendCloseACK();
                closeThread.interrupt();
                shouldRun = false;
                break;

            case SYN_ACK:
                if(isConnected()) {
                    System.out.println("Duplicate SYN_ACK");
                    sendSYNACK(id);
                    return;
                }

                id = input.getInt();
                oWindow = new OutgoingWindow(input.getInt());

                System.out.println("Wow we got SYN_ACK lets send one back :D ");
                sendSYNACK(id);
                synThread.interrupt();
                break;

            case ACK:
                int packetId = input.getInt();
                System.out.println("Received ACK on packet " + packetId);
                oWindow.receive(packetId);
                update();
                break;
        }
    }

}
