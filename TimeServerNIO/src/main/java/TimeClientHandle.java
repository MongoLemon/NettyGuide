import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by AnthonySU on 9/30/15.
 */
public class TimeClientHandle implements Runnable {
    private String host;
    private int port;
    private Selector selector;
    private SocketChannel socketChannel;
    private volatile boolean stop;

    public TimeClientHandle( String host, int port) {
        this.host = host == null ? "127.0.0.1" : host;
        this.port = port;
        try {
            selector = Selector.open();
            socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void run() {
        try {
            // try to connect and send request to TimeServer
            doConnect();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        while(!stop) {
            // similar to TimeServer, read input/response from TimeServer
            try {
                selector.select(1000);
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iter = selectedKeys.iterator();
                SelectionKey key = null;
                while(iter.hasNext()) {
                    key = iter.next();
                    iter.remove();
                    try {
                        handleInput(key);
                    } catch (Exception e) {
                        if(key!=null) {
                            key.cancel();
                            if(key.channel() != null) {
                                key.channel().close();
                            }
                        }
                    }
                }
            } catch (IOException e){
                e.printStackTrace();
                System.exit(1);
            }
        }

        if(selector!=null){
            try{
                selector.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void doConnect() throws IOException {
        // if successfully connected, register socketChannel to selector, send request
        if(socketChannel.connect(new InetSocketAddress(host,port))) {
            socketChannel.register(selector, SelectionKey.OP_READ); // register it readable
            doWrite(socketChannel);
        } else {
            socketChannel.register(selector, SelectionKey.OP_CONNECT);
        }
    }

    private void doWrite(SocketChannel sc) throws IOException {
        byte[] req = "QUERY TIME ORDER".getBytes();
        ByteBuffer writeBuffer = ByteBuffer.allocate(req.length);
        writeBuffer.put(req);
        writeBuffer.flip();
        sc.write(writeBuffer);
        if(!writeBuffer.hasRemaining()) {
            System.out.println("Send order to server succeed");
        }
    }

    private void handleInput(SelectionKey key) throws IOException {
        if (key.isValid()) {

            // make sure the connection is successful
            SocketChannel sc = (SocketChannel)key.channel();

            if(key.isConnectable()) {
                if(sc.finishConnect()) {
                    sc.register(selector, SelectionKey.OP_READ); // register it readable
                    doWrite(sc); // ? why we need doWrite here ? Is it used for making repeated requests ?
                }else {
                    System.exit(1);
                }
            }
            if(key.isReadable()) {
                ByteBuffer readBuffer = ByteBuffer.allocate(1024);
                int readBytes = sc.read(readBuffer);

                if(readBytes > 0) {
                    readBuffer.flip();
                    byte[] bytes = new byte[readBuffer.remaining()]; // create a destination array big enough to store readBuffer
                    readBuffer.get(bytes); // this method transfer what inside buffer to destination array

                    String body = new String(bytes, "UTF-8");
                    System.out.println("Now is : " + body);
                    this.stop = true;
                } else if (readBytes < 0) {
                    key.cancel();
                    sc.close();
                } else
                    ;
            }
        }
    }

}
