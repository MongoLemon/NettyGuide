import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Created by AnthonySU on 9/23/15.
 */

/**
 * Client 通过Socket 创建 通过 PrintWriter out 发送查询时间的"QUERY TIME ORDER"指令, 然后
 * 通过in.readLine() 读取Server 的 response 并将结果打印, 随后关闭链接
 * 释放资源
 */
public class TimeClient {

    public static void main(String[] args) {
        int port = 8080;
        if(args != null && args.length > 0) {
            try {
                port = Integer.valueOf(args[0]);
            } catch (NumberFormatException e) {

            }
        }


        Socket socket =null;
        BufferedReader in = null;
        PrintWriter out = null;

        try {
            socket = new Socket("localhost", port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            out.println("QUERY TIME ORDER");
            System.out.println("Send order to server succeed.");
            String resp = in.readLine();
            System.out.println("Now is : " + resp);
         } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(out != null) {
                out.close();
                out = null;
            }
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                socket = null;
            }
        }
    }
}
