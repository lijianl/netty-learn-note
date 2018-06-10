package com.alibaba.dubbo.performance.demo.agent.netty.demo;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * echo-handler
 */
class EchoHandler extends SimpleChannelInboundHandler<Object> {

    private Logger logger = LoggerFactory.getLogger(EchoHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext chc, Object o) throws Exception {
        /*System.out.println("server received  " + o.toString() + "thread: " + chc.channel().eventLoop().toString());*/
        chc.writeAndFlush(o);
        logger.info("server receive {}", o.toString());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}