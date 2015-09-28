import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by AnthonySU on 9/23/15.
 */

/**
 * TimeServer 根据传入的参数设置监听端口, 如果没有参数传入则默认8080.
 * 同时通过过无限循环来监听 Client 的链接.
 * 如果没有 Client 接入, 则主线程阻塞在ServerSocket.accept()
 * 当有 Client 接入的时候, 构造TimeServerHandler对象,使用它为参数创建一个新的 Client 线程 处理这条 Socket 链路
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

                /**
                 * below is the way of execute pool solution
                 */
//                TimeServerHandlerExecutePool singleExecutor = new TimeServerHandlerExecutePool(50, 10000);
//                while (true) {
//                    socket =  server.accept();
//                    singleExecutor.execute(new TimeServerHandler(socket));
//                }
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
