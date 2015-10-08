import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

import java.util.Date;

/**
 * Created by AnthonySU on 10/4/15.
 */

/**
 * TimeServerHandler 对网络时间进行读写操作, 注意channelRead 和 exceptionCaught
 * ByteBuf的readableBytes方法可以获取缓冲区可读的字节数,创建byte数组,通过readBytes方法将缓冲区中的字节数字复制到新建数组中去
 * 通过ChannelHandlerContext 的 write方法异步发送消息给客户端
 *
 * flush方法的作用是将消息发送队列中的消息写入到SocketChannel中发送给对方.从性能角度考虑,为了防止频繁地唤醒Selector
 * Netty的write方法并不直接将消息写入SocketChannel中,调用write方法只是吧待发送的消息放到发送缓冲数组中,调用flush将发送
 * 的消息全部写入到SocketChannel中
 */
public class TimeServerHandler extends ChannelHandlerAdapter {

    private int counter;

    @Override
    public void channelRead (ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;
        byte[] req = new byte[buf.readableBytes()];
        buf.readBytes(req);
        String body = new String(req, "UTF-8").substring(0,req.length - System.getProperty("line.separator").length());

        System.out.println("The Time Server receive order : " + body + "the counter is : " + ++counter);

        String currentTime = "QUERY TIME ORDER".equalsIgnoreCase(body) ? new Date(
                System.currentTimeMillis()
        ).toString() : "BAD ORDER";
        currentTime = currentTime + System.getProperty("line.separator");
        ByteBuf resp = Unpooled.copiedBuffer(currentTime.getBytes());
        ctx.write(resp);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
    }
}

