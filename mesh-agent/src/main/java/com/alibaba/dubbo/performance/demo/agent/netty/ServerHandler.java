package com.alibaba.dubbo.performance.demo.agent.netty;

import com.alibaba.dubbo.performance.demo.agent.dubbo.RpcClient;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 处理Nrequest请求
 */
public class ServerHandler extends SimpleChannelInboundHandler<NRequest> {

    private Logger logger = LoggerFactory.getLogger(ServerHandler.class);

    private RpcClient rpcClient;

    public ServerHandler() {
        rpcClient = new RpcClient();
    }


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, NRequest request) throws Exception {
        logger.debug("Receive request " + request.getRequestId());
        NResponse response = new NResponse();
        response.setRequestId(request.getRequestId());
        try {
            Object result = handle(request);
            response.setResult(result);
        } catch (Throwable t) {
            logger.error("RPC Server handle request error", t);
        }
        // 发送返回结果
        ctx.writeAndFlush(response).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                logger.info("provider-agent-return:{}:{}", response.getRequestId(), response.getResult());
            }
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("server caught exception", cause);
        ctx.close();
    }

    /**
     * 具体的处理请求:provider-agent调用dubbo服务
     */
    private Object handle(NRequest request) throws Throwable {
        String requestId = request.getRequestId();
        logger.info("provider-agent-deal-with:{}", requestId);
        String interfaceName = request.getInterfaceName();
        String methodName = request.getMethodName();
        String parameterTypesString = request.getParameterTypesString();
        String parameter = request.getParameter();
        return rpcClient.invoke(interfaceName, methodName, parameterTypesString, parameter);
    }
}
