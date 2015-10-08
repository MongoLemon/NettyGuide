import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;


/**
 * Created by AnthonySU on 10/4/15.
 */

/**
 * NioEventLoopGroup 是线程组,包含一组 NIO 线程,专门用于处理网络事件,本质上是Reactor线程组.
 * 创建的两个Group中的一个线程组用于服务端接收客户端的连接
 *                  另一个线程组用于进行SocketChannel网络读写
 *
 * ServerBootstrap她是netty 启动 NIO 服务的辅助启动类, 其group方法将两个NIO 线程组参数传入.
 * 接着创建的Channel为NioServerSocketChannel 和 JDK NIO 类库中的ServerSocketChannel是一个故事.
 * 接着配置Channel的TCP 参数
 * 最后绑定I/O事件的处理类ChildChannelHandler --> Reactor模式中的handler类, 作用是如记录日志,编码消息等
 *
 * 调用ServerBootstrap的 bind 方法监听端口,然后调用sync来等待绑定操作完成. 完成后Netty会返回一个ChannelFuture,
 * 用于异步操作的通知回调
 *
 * 同理closeFuture().sync()方法进行阻塞,等待服务端链路关闭后 main() 函数才退出
 */
public class TimeServer {

    public void bind(int port) throws Exception {

        // deploy NIO threads of server
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .childHandler(new ChildChannelHandler());

            // bind the port, sync and wait for its success
            ChannelFuture f = b.bind(port).sync();

            // wait for listing port close from server side
            f.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    private class ChildChannelHandler extends ChannelInitializer<SocketChannel> {

        @Override
        protected void initChannel(SocketChannel arg0) throws Exception {
            arg0.pipeline().addLast(new LineBasedFrameDecoder(1024));
            arg0.pipeline().addLast(new StringDecoder());
            arg0.pipeline().addLast(new TimeServerHandler());
        }
    }

    public static void main (String[] args) throws Exception {
        int port = 8080;
        if(args != null && args.length > 0 ) {
            try {
                port = Integer.valueOf(args[0]);
            } catch (NumberFormatException e) {
                // use default port
            }
        }
        new TimeServer().bind(port);
    }
}
