package com.alibaba.dubbo.performance.demo.agent.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * provider-agent 启动的服务
 */
public class NServer {

    private Logger logger = LoggerFactory.getLogger(NServer.class);
    private String host;
    private Integer port;

    private int DEFAULT_IO_THREADS = Math.min(Runtime.getRuntime().availableProcessors() + 1, 32);

    private ServerBootstrap bootstrap;
    private NioEventLoopGroup bossGroup;
    private NioEventLoopGroup workerGroup;

    public NServer(String host, Integer port) {
        this.host = host;
        this.port = port;
    }

    public void start() {
        bootstrap = new ServerBootstrap();
        bossGroup = new NioEventLoopGroup(1, new DefaultThreadFactory("NettyServerBoss", false));
        workerGroup = new NioEventLoopGroup(DEFAULT_IO_THREADS, new DefaultThreadFactory("NettyServerWorker", true));
        //workerGroup.setIoRatio(70);
        try {

            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .option(ChannelOption.SO_REUSEADDR, Boolean.TRUE)
                    .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .childOption(ChannelOption.TCP_NODELAY, Boolean.TRUE)
                    .childOption(ChannelOption.SO_KEEPALIVE, Boolean.TRUE)
                    .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .childHandler(new ChannelInitializer<NioSocketChannel>() {
                        @Override
                        protected void initChannel(NioSocketChannel ch) throws Exception {
                            ch.pipeline()
                                    .addLast(new NDecoder(NRequest.class))
                                    .addLast(new NEncoder(NResponse.class))
                                    //.addLast(new ProtobufDecoder())
                                    .addLast(new ServerHandler());
                        }
                    });
            // bind
            ChannelFuture bind = bootstrap.bind(host, port);
            logger.info("PA start on port {}", port);
            bind.syncUninterruptibly().channel().closeFuture().sync();

        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }

    }

    public static void main(String[] args) {
    }
}
