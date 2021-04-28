import java.awt.image.DataBuffer;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * Lab 3 - Client
 *
 * @author Johan Karlsson (jkn18010) & Ludwig Johansson (ljn18012)
 *
 * The entry point for the Client. Creates a Connection object which is used to manage the connection to the server. Also reads messages from the user
 * and sends to the server.
 */
public class Main {
    public static void main(String[] args) throws Exception {
        var address = InetAddress.getByName("127.0.0.1"); //the address that we want to connect to
        int port = 54322; //the port that we want to connect to

        System.out.println("---Client---");
        Connection conn = new Connection(address, port);
        new Thread(conn).start();

        Scanner sc = new Scanner(System.in);
        while(true) {
            var message = sc.nextLine(); //read message from the user

            if(message.equalsIgnoreCase("close")) {
                System.out.println("We will attempt to close the connection 5 times before giving up.");
                conn.close();
                break;
            }

            List<String> stringParts = new ArrayList<String>();

            for(int i = 0; i < message.length(); i += 8) { //split string into pieces of 8
                int limit = Math.min(message.length(), i + 8);
                String s = message.substring(i, limit);
                stringParts.add(s);
            }

            var output = ByteBuffer.allocate(4);
            output.putInt(stringParts.size());
            conn.sendData(output.array());

            System.out.println("Sending " + (stringParts.size() + 1) + " packets.");
            stringParts.forEach(s -> {
                var bytes = s.getBytes();
                System.out.println(new String(bytes));
                conn.sendData(s.getBytes());
            });
        }
    }
}
