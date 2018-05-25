package com.alibaba.dubbo.performance.demo.agent.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 发送request请求
 */
public class ClientHandler extends SimpleChannelInboundHandler<NResponse> {

    private Bootstrap bootstrap;
    private ChannelFuture channel;
    private String host;
    private Integer port;
    private NResponse response;

    private Logger logger = LoggerFactory.getLogger(ClientHandler.class);

    public ClientHandler(String host, Integer port) {
        this.host = host;
        this.port = port;
    }

    /**
     * 读取接口的结果
     */
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, NResponse nResponse) throws Exception {
        this.response = response;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("api caught exception", cause);
        ctx.close();
    }

    /**
     * consumer-agent发送请求到provider-agent
     */
    public NResponse send(NRequest request) throws Exception {
        try {
            ChannelFuture future = getChannel();
            logger.info("connect-RPC-server-ok:{}:{}", host, port);
            //请求数据，关闭链接
            Channel channel = future.channel();
            channel.writeAndFlush(request).sync();
            channel.closeFuture().sync();
            //返回请求结果
            return response;
        } finally {
            bootstrap.clone();
        }
    }

    public ChannelFuture getChannel() throws Exception {
        if (null != channel) {
            return channel;
        }

        if (null == bootstrap) {
            initBootstrap();
        }
        if (null == channel) {
            channel = bootstrap.connect(host, port).sync();
        }
        return channel;
    }


    public void initBootstrap() {
        EventLoopGroup eventLoopGroup = new NioEventLoopGroup(10);
        bootstrap = new Bootstrap()
                .group(eventLoopGroup)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.ALLOCATOR, UnpooledByteBufAllocator.DEFAULT)
                .channel(NioSocketChannel.class);

        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel channel) throws Exception {
                ChannelPipeline pipeline = channel.pipeline();
                pipeline.addLast(new NEncoder(NRequest.class));
                pipeline.addLast(new NDecoder(NResponse.class));
                pipeline.addLast(ClientHandler.this);
            }
        });
    }
}
