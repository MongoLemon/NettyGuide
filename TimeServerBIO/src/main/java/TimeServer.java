import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by AnthonySU on 9/23/15.
 */
public class TimeServer {

    public static void main(String[] args) throws IOException {
        int port = 8080;
        if(args!=null && args.length>0) {

            try {
                port = Integer.valueOf(args[0]);
            } catch (NumberFormatException e) {
                // make it default value
            }
        }

        ServerSocket server = null;
        try {
            server = new ServerSocket(port);
            System.out.println("The time server is start in port: " + port);
            Socket socket;
            while(true) {
                socket = server.accept();
                new Thread(new TimeServerHandler(socket)).start(); // TimeServerHandler is a Runnable, create a new Thread here for current Socket
            }
        } finally {
            if(server != null) {
                System.out.println("The time server close");
                server.close();
                server = null;
            }
        }
    }
}
