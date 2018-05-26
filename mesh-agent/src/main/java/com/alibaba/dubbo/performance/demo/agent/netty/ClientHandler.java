package com.alibaba.dubbo.performance.demo.agent.netty;

import io.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 发送request请求
 */
public class ClientHandler extends SimpleChannelInboundHandler<NResponse> {

    private Logger logger = LoggerFactory.getLogger(ClientHandler.class);

    /**
     * 读取接口的结果
     */
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, NResponse response) throws Exception {
        logger.info("consumer read :{}:{}", response.getRequestId(), response.getResult());
        String requestId = response.getRequestId();
        NFuture future = NRequestHolder.get(requestId);
        if (null != future) {
            // 清空静态内存
            NRequestHolder.remove(requestId);
            future.done(response);
        }
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("api caught exception", cause);
        ctx.close();
    }
}
