package com.alibaba.dubbo.performance.demo.agent.netty.demo;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NettyClient {

    private static Bootstrap bootstrap = null;
    private static String host = "127.0.0.1";
    private static int port = 40000;

    public static Channel getChannel() throws InterruptedException {

        if (null == bootstrap) {
            synchronized (NettyClient.class) {
                if (bootstrap == null) {
                    EventLoopGroup eventLoopGroup = new EpollEventLoopGroup(200);
                    bootstrap = new Bootstrap()
                            .group(eventLoopGroup)
                            .option(ChannelOption.SO_KEEPALIVE, true)
                            .option(ChannelOption.TCP_NODELAY, true)
                            .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                            .channel(EpollSocketChannel.class)
                            .handler(new ChannelInitializer<SocketChannel>() {
                                @Override
                                public void initChannel(SocketChannel channel) throws Exception {
                                    ChannelPipeline pipeline = channel.pipeline();
                                    pipeline.addLast(new EchoHandler());
                                }
                            });
                    System.out.println("client-" + host + ":" + port);
                }
            }
        }

        return bootstrap.connect(host, port).sync().channel();
    }

    public static void main(String[] args) throws InterruptedException {
        Channel channel = getChannel();
        while (true) {
            channel.writeAndFlush("哈哈");
        }
    }


}
