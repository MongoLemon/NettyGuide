import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by AnthonySU on 9/28/15.
 * MTimeServer的构造方法, 资源初始化,创建多路复用器Selector, ServerSocketChannel.
 * 将ServerSocketChannel设置为 异步非阻塞模式, backlog=1024.
 * 系统资源初始化后,将SeverSocketChannel注册到Selector, 监听SelectionKey.OP_ACCEPT操作位, 如果资源初始化失败则退出
 *
 * 在线程run方法中, while 循环体中循环遍历selector, 休眠时间为1s = 每隔1s被唤醒一次. 当有就绪状态的Channel 时,
 * selector将返回就绪Channel的SelectionKey set,对之进行iterate 来进行为网络的异步读写操作.
 *
 * 在handleInput方法中 处理新接入的客户端请求消息, 根据SelectionKey的位操作来判断网络时间的类型.
 * 通过ssc de accept()来接收客户端的链接请求并创建SocketChannel实例 --> 这一过程相当于完成TCP的三次握手,建立TCP物理链接.
 * 这里TCP可以设置接收/发送缓冲区大小,但这里还没有设置.
 *
 * 随后创建了ByteBuffer进行客户端请求消息的读取.这里设置了大小为1K的缓冲区.
 * 再调用sc.read(readBuffer) 读取请求码流.这个过程异步非阻塞,使用返回值进行判断,根据读取到的字节数,返回值有三种可能结果:
 * 1. >0 : 读到了字节,对字节进行编码
 * 2. =0 : 没有读取到字节,属于正常场景, 忽略
 * 3. <0 : -1 的case,链路关闭, key cancel, socketChannel 关闭释放资源
 *
 * 在第 1 中case中,进行解码. 对readBuffer进行flip() 操作, flip的作用是将当前缓冲区当前的limit设置为position, position重置为0.
 * 然后根据缓冲区的可读字节个数创建字节数组, readBuffer.get()操作将缓冲区可读的字节数组复制到新创建的字节数组中, 然后打印.
 *
 * doWrite 方法中将response异步发送给客户端. 首先将response编码成bytes, 并根据其大小创建ByteBuffer,
 * 并用put(bytes)方法将字节组复制到缓冲区. 然后flip. 最后调用SocketChannel write方法将缓冲区中的字节组发送出去.
 * "半包" 问题还没有解决
 */
public class MultiplexerTimeServer implements Runnable {

    private Selector selector;

    private ServerSocketChannel servChannel;

    private volatile boolean stop;

    /**
     * initiate selector and bind the port
     */
    public MultiplexerTimeServer(int port) {
        try {
            selector = Selector.open();
            servChannel = ServerSocketChannel.open();
            servChannel.configureBlocking(false); // non-blocking
            servChannel.socket().bind(new InetSocketAddress(port), 1024);
            servChannel.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("The Time server starts @port : " + port);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void stop() {
        this.stop = true;
    }

    /**
     * MultiplexerTimeServer will run and polling out activated channel
     */

    public void run() {
        // iterate selector, sleep 1s
        while(!stop) {
            try {
                selector.select(1000);
                Set<SelectionKey> selectionKeySet = selector.selectedKeys();
                Iterator<SelectionKey> iter = selectionKeySet.iterator();
                SelectionKey key = null;
                while(iter.hasNext()) {
                    key = iter.next();
                    iter.remove();  // remove the current key from the queue
                    try {
                        handleInput(key);
                    } catch (Exception e) {
                        if (key != null) {
                            key.cancel();
                            if (key.channel() != null) {
                                key.channel().close();
                            }
                        }
                    }
                }

            } catch (Throwable t) {
                t.printStackTrace();
            }
        }

        // if selector is stop, Channels and Pipes registered in Selector will be removed automatically
        if(selector != null) {
            try {
                selector.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // handle Input data by recognizing the selectionKey
    private void handleInput (SelectionKey key) throws IOException {
        if(key.isValid()) {
            // tackle the incoming request
            if (key.isAcceptable()) {
                // accept the new connection
                ServerSocketChannel ssc = (ServerSocketChannel)key.channel();
                SocketChannel sc = ssc.accept();
                sc.configureBlocking(false); // non-blocking

                // add the new connection to the selector
                sc.register(selector, SelectionKey.OP_READ);
            }


            if(key.isReadable()) {
                // Read the data
                SocketChannel sc = (SocketChannel) key.channel();
                ByteBuffer readBuffer = ByteBuffer.allocate(1024);
                int readBytes = sc.read(readBuffer);
                if (readBytes > 0) {
                    readBuffer.flip();
                    byte[] bytes = new byte[readBuffer.remaining()];
                    readBuffer.get(bytes); // this method transfer what inside buffer to destination array

                    // transfer the bytes to string body and print out
                    String body = new String(bytes, "UTF-8");
                    System.out.println("The time server receive order : " + body);

                    String currentTime = "QUERY TIME ORDER".equalsIgnoreCase(body) ? new Date(System.currentTimeMillis()).toString()
                            : "BAD ORDER";
                    doWrite(sc, currentTime);
                }else if(readBytes < 0) {
                    // close SocketChannel
                    key.cancel();
                    sc.close();
                } else
                    ; // 0 byte, ignore
            }
        }
    }

    // tackle output
    private void doWrite(SocketChannel channel, String response) throws IOException {
        if (response != null && response.trim().length() > 0) {
            byte[] bytes = response.getBytes();
            ByteBuffer writeBuffer = ByteBuffer.allocate(bytes.length);
            writeBuffer.put(bytes);
            writeBuffer.flip();
            channel.write(writeBuffer);
        }
    }
}
