import java.io.IOException;

/**
 * Created by AnthonySU on 10/1/15.
 */
public class TimeServer {

    public static void main(String[] args) throws IOException {

        int port = 8080;
        if(args != null && args.length >0) {
            try {
                port = Integer.valueOf(args[0]);
            } catch (NumberFormatException e){
                // use the default port value
            }
        }

        AsyncTimeServerHandler timeServer = new AsyncTimeServerHandler(port);
        new Thread(timeServer, "AIO-AsyncTimeServerHandler-001").start();
    }
}
