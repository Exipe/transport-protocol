import java.util.ArrayList;
import java.util.HashSet;

/**
 * Lab 3 - Server
 *
 * @author Johan Karlsson (jkn18010) & Ludwig Johansson (ljn18012)
 *
 * This class is implements a sliding window for incoming packets only
 */
public class IncomingWindow {
    private int size;
    private int counter = 0;
    private ArrayList<IncomingPacket> packets = new ArrayList<IncomingPacket>();
    private PacketReceiver receiver;

    public IncomingWindow(int size, PacketReceiver receiver) {
        this.size = size;
        this.receiver = receiver;
    }

    public int getSize() {
        return size;
    }

    public void addPacket(IncomingPacket incomingPacket) {
        int packetId = incomingPacket.getId();

        //if the packetId we add is the one we expect, we can send it up instantly.
        if(packetId == counter) {
            sendUp(incomingPacket);
            counter = (counter + 1) % (size * 2);

            //loop through the packets that we have and see if any of them are next in line to be sent up.
            while(true) {
                var op = packets.stream().filter(p -> p.getId() == counter).findFirst();
                if(op.isEmpty()) break;

                sendUp(op.get());
                packets.remove(op.get());
                counter = (counter + 1) % (size * 2);
            }
        }
        else if(validateID(packetId)) {
            packets.add(incomingPacket);
        }

        /*var checked = new HashSet<Integer>();
        for(IncomingPacket p : packets) {
            System.out.print(p.getId() + " ");

            if(checked.contains(p.getId())) {
                System.out.print(" ||WARNING DUPLICATE|| ");
            }

            checked.add(p.getId());
        }

        if(!checked.isEmpty()) {
            System.out.println();
        }*/
    }

    private void sendUp(IncomingPacket incomingPacket) {
        receiver.receivePacket(incomingPacket);
    }

    //this method checks if the packetId is in the expected interval.
    private boolean validateID(int id) {
        if(packets.stream().anyMatch(p -> p.getId() == id)) {
            return false;
        }

        int highest = (counter+size) % (2*size);

        if(counter < highest) {
            return id >= counter && id < highest;
        } else {
            return !(id >= highest && id < counter);
        }
    }

}
