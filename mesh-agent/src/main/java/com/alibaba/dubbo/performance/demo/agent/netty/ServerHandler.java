package com.alibaba.dubbo.performance.demo.agent.netty;

import com.alibaba.dubbo.performance.demo.agent.dubbo.RpcClient;
import io.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 处理Nrequest请求
 */

@ChannelHandler.Sharable
public class ServerHandler extends SimpleChannelInboundHandler<NRequest> {

    private Logger logger = LoggerFactory.getLogger(ServerHandler.class);

    private RpcClient rpcClient = null;

    private ExecutorService service = Executors.newFixedThreadPool(300);

    public ServerHandler() {
        rpcClient = new RpcClient();
    }


    /**
     * 优化方案:
     * 1. clint-server启动多个channel[上限1024]
     * + 占用太多端口，意味着部署少的服务
     * 2. channel里面处理请求是多线程并发
     * + 不能在所有的channnel里面实现多线程
     * + TPS:1k
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, NRequest request) throws Exception {

        /**
         * 业务线程池
         */
        logger.info("PA accept at:{}:{}", request.getRequestId(), System.currentTimeMillis());

        service.execute(() -> {

            long start = System.currentTimeMillis();
            logger.info("PA start at {}:{}", request.getRequestId(), start);
            Channel channel = ctx.channel();
            NResponse response = new NResponse();
            response.setRequestId(request.getRequestId());
            try {
                Object result = handle(request);
                response.setResult(result);
            } catch (Throwable t) {
                logger.error("PA handle request error", t);
            }
            logger.info("PA1:{}:{}", response.getRequestId(), System.currentTimeMillis() - start);
            channel.writeAndFlush(response).addListeners(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture channelFuture) throws Exception {
                    long end = System.currentTimeMillis();
                    logger.info("PA2:{}:{}", response.getRequestId(), end - start, end);
                }
            });

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
        String interfaceName = request.getInterfaceName();
        String methodName = request.getMethodName();
        String parameterTypesString = request.getParameterTypesString();
        String parameter = request.getParameter();
        return rpcClient.invoke(interfaceName, methodName, parameterTypesString, parameter);
    }
}
