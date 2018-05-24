package com.alibaba.dubbo.performance.demo.agent.netty;

import io.netty.bootstrap.Bootstrap;
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
     * 发送请求
     */
    public NResponse send(NRequest request) throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group);
            bootstrap.channel(NioSocketChannel.class);
            bootstrap.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel channel) throws Exception {
                    ChannelPipeline pipeline = channel.pipeline();
                    pipeline.addLast(new NEncoder(NRequest.class));
                    pipeline.addLast(new NDecoder(NResponse.class));
                    pipeline.addLast(ClientHandler.this);
                }
            });
            bootstrap.option(ChannelOption.TCP_NODELAY, true);
            ChannelFuture future = bootstrap.connect(host, port).sync();
            logger.info("connect-RPC-server-ok:{}:{}", host, port);
            //请求数据，关闭链接
            Channel channel = future.channel();
            channel.writeAndFlush(request).sync();
            channel.closeFuture().sync();
            //返回请求结果
            return response;
        } finally {
            group.shutdownGracefully();
        }
    }


}
