import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by AnthonySU on 9/23/15.
 */

/**
 * 如果请求为"QUERY TIME ORDER", 则获取最新的系统时间,通过PrintWrite out发送给 Client
 */
public class TimeServerHandler implements Runnable {

    private Socket socket;
    public TimeServerHandler(Socket socket) {
        this.socket = socket;
    }


    public void run() {
        BufferedReader in  =null;
        PrintWriter out = null;

        try {
            in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
            out = new PrintWriter(this.socket.getOutputStream(), true); // auto flush

            String currentTime;
            String body;
            while(true){
                body = in.readLine();  // get read every line of input stream
                if(body == null){
                    break;
                }

                System.out.println("The time server receive order : " + body);
                currentTime = "QUERY TIME ORDER".equalsIgnoreCase(body)? new Date(System.currentTimeMillis()).toString() : "BAD ORDER";
                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date printDate;

                try {
                    printDate = dateFormat.parse(currentTime);
                    out.println(printDate);
                } catch (ParseException e) {
                    out.println("BAD ORDER");
                }
            }
        }  catch (IOException e) {

            if(in != null) {
                try {
                    in.close();
                } catch (IOException e1) {
                    e.printStackTrace();
                }
            }

            if(out != null) {
                out.close();
                out = null;
            }

            if(this.socket != null) {
                try {
                    this.socket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                this.socket = null;
            }
        }
    }
}
