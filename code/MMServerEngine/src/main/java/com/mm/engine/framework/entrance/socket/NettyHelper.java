package com.mm.engine.framework.entrance.socket;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

/**
 * Created by a on 2016/8/29.
 */
public class NettyHelper {
    private static final Logger log = LoggerFactory.getLogger(NettyHelper.class);
    public static synchronized Channel createAndStart(
            final int port,
            final ChannelHandler encoder,
            final ChannelHandler decoder,
            final ChannelHandler handler,
            String entranceName
    ) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        NettyThread nettyThread = new NettyThread(port,encoder,decoder,handler,entranceName,latch);
        nettyThread.start();
        latch.await(); // 等待netty启动再放出它
        return nettyThread.getChannel();
    }
    private static class NettyThread extends Thread{
        private Channel channel = null;

        final int port;
        final ChannelHandler encoder;
        final ChannelHandler decoder;
        final ChannelHandler handler;
        final String entranceName;
        final CountDownLatch latch;

        public Channel getChannel() {
            return channel;
        }

        public NettyThread(final int port,
                           final ChannelHandler encoder,
                           final ChannelHandler decoder,
                           final ChannelHandler handler,
                           String entranceName,
                           CountDownLatch latch){
            this.port = port;
            this.encoder = encoder;
            this.decoder = decoder;
            this.handler = handler;
            this.entranceName = entranceName;
            this.latch = latch;
        }
        @Override
        public void run() {
            EventLoopGroup bossGroup = new NioEventLoopGroup(); // (1)
            EventLoopGroup workerGroup = new NioEventLoopGroup();
            try {
                ServerBootstrap b = new ServerBootstrap(); // (2)
                b.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class) // (3)
                        .childHandler(new ChannelInitializer<SocketChannel>() { // (4)
                            @Override
                            public void initChannel(SocketChannel ch) throws Exception {
                                ch.pipeline().addLast(
                                        decoder, // 解码器
                                        encoder, // 编码器
                                        handler //处理器
                                );
                            }
                        })
                        .option(ChannelOption.SO_BACKLOG, 128)          // (5)
                        .option(ChannelOption.TCP_NODELAY, true)
                        .childOption(ChannelOption.SO_KEEPALIVE, true); // (6)
                // Bind and start to accept incoming connections.
                ChannelFuture f = b.bind(port); // (7)
                f.sync();
                channel = f.channel();
                latch.countDown();
                log.info(entranceName+" netty start end");
                // Wait until the server socket is closed.
                // In this example, this does not happen, but you can do that to gracefully
                // shut down your server.
                f.channel().closeFuture().sync();

                log.info(entranceName+"netty stop ");
            }catch (Exception e){
                e.printStackTrace();
            }finally {
                workerGroup.shutdownGracefully();
                bossGroup.shutdownGracefully();
            }
        }
    }
}