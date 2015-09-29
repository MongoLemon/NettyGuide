import java.io.IOException;

/**
 * Created by AnthonySU on 9/28/15.
 */
public class TimeServer {

    public static void main (String[] args) throws IOException {
        int port = 8080;
        if (args !=null && args.length > 0) {
            try {
                 port = Integer.valueOf(args[0]);
            } catch (NumberFormatException e) {
                // port default number is 8080
            }
        }

        MultiplexerTimeServer timeServer = new MultiplexerTimeServer(port);

        // create a new thread for the timeServer
        new Thread(timeServer, "NIO-MultiplexerTimeServer-001").start(); // use one thread to manipulate selector server
    }
}

