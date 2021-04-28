/**
 * Lab 3 - Server
 *
 * @author Johan Karlsson (jkn18010) & Ludwig Johansson (ljn18012)
 *
 * A class for defining an IncomingPacket
 */
public class IncomingPacket {

    private int id;
    private byte[] data;

    public IncomingPacket(int id, byte[] data) {
        this.id = id;
        this.data = data;
    }

    public int getId() {
        return id;
    }

    public byte[] getData() { return data; }

}
