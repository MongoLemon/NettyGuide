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
            socketChannel.configureBlocking(false); // 设置socketChannel为非阻塞模式
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void run() {
        try {
            // 链接server发送请求,这里没有进行重连操作,所以放在了循环的前面
            doConnect();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        // 轮询Selector 上的 Channel
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

        // 在线程退出后,进行selector资源的释放
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
        // 首先判断SocketChannel的链接是否成功
        if(socketChannel.connect(new InetSocketAddress(host,port))) {
            // 将SocketChannel注册到Selector上,为readable
            socketChannel.register(selector, SelectionKey.OP_READ);
            doWrite(socketChannel);
        } else {
            // 如果上一步失败,意味着没有直接链接成功,server没有返回TCP握手应答消息
            // 但并不表示链接失败, 所以将SocketChannel注册为connect, 当server返回了
            // TCP SYN-ACK后, Selector轮询到这个SocketChannel是就发现其为链接就绪状态
            socketChannel.register(selector, SelectionKey.OP_CONNECT);
        }
    }

    private void doWrite(SocketChannel sc) throws IOException {
        // req就是我们要发送的消息内容,进行编码,放入缓冲区,write()发送
        byte[] req = "QUERY TIME ORDER".getBytes();
        ByteBuffer writeBuffer = ByteBuffer.allocate(req.length);
        writeBuffer.put(req);
        writeBuffer.flip();
        sc.write(writeBuffer);
        // 因为是异步IO,所以会有"半包"问题, 通过hasRemaining()对发送结果进行判断消息是否发送完全
        if(!writeBuffer.hasRemaining()) {
            System.out.println("Send order to server succeed");
        }
    }

    private void handleInput(SelectionKey key) throws IOException {

        // 首先判断SelectionKey是出于什么状态
        if (key.isValid()) {

            // make sure the connection is successful
            SocketChannel sc = (SocketChannel)key.channel();

            // 如果出于连接状态,说明server已经返回了ACK应答, 这个时候需要对结果进行判断
            if(key.isConnectable()) {

                // 这里调用SocketChannel.finishConnet(), 返回值为true则说明客户端链接成功
                if(sc.finishConnect()) {
                    //将这个SocketChannel注册为 read操作位
                    sc.register(selector, SelectionKey.OP_READ); // register it readable
                    // 然后发送请求给server
                    doWrite(sc); // ? why we need doWrite here ? Is it used for making repeated requests ?
                }else {
                    System.exit(1);
                }
            }

            // 如果Client 接收到了 server的response, 则SocketChannel是可读的状态
            if(key.isReadable()) {

                // 预先不知道码流大小,先分配1M 接收缓冲区
                ByteBuffer readBuffer = ByteBuffer.allocate(1024);

                // read()方法进行异步读取操作
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
