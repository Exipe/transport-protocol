
import java.util.ArrayList;
import java.util.List;

/**
 * Lab 3 - Client
 *
 * @author Ludwig Johansson (ljn18012), Johan Karlsson (jkn18010)
 *
 * Implements a sliding window for outgoing packets
 */
public class OutgoingWindow {

    private int counter = 0;
    private final int size;

    private final List<OutgoingPacket> window = new ArrayList<>();

    public OutgoingWindow(int size) {
        this.size = size;
    }

    public boolean hasRoom() {
        return window.size() < size;
    }

    public int nextId() {
        return counter;
    }

    public void add(OutgoingPacket packet) {
        window.add(packet);
        counter = (counter + 1) % (size * 2);
    }

    public void receive(int id) {
        //check if the packet of this id exists, if so, mark it as ACK:ed
        window.stream().filter(p -> p.id() == id).findAny().ifPresent(OutgoingPacket::ack);

        while(window.size() > 0 && window.get(0).isAcked()) {
            window.remove(0);
        }
    }

}
