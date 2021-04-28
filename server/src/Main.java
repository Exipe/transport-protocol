
/**
 * Lab 3 - Server
 *
 * @author Ludwig Johansson (ljn18012), Johan Karlsson (jkn18010)
 *
 * Starts the server
 */
public class Main {

    public static void main(String[] args) throws Exception {
        System.out.println("---Server---");
        final var transportLayer = new TransportLayer(54322);
        new Thread(transportLayer).start();
    }

}
