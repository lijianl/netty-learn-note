package com.alibaba.dubbo.performance.demo.agent.netty.demo;


import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * echo-handler
 */
class StringHandler extends SimpleChannelInboundHandler<Object> {

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {
        channelHandlerContext.writeAndFlush(o);
        System.out.println("obj =" + o.toString());
    }
}