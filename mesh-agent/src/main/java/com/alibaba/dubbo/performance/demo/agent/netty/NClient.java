package com.alibaba.dubbo.performance.demo.agent.netty;

import com.alibaba.dubbo.performance.demo.agent.registry.Endpoint;
import com.alibaba.dubbo.performance.demo.agent.registry.IRegistry;
import com.google.common.collect.Maps;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * consumer-agent 代理启动
 * 封装请求, 封装loadbalance
 */
public class NClient {

    private Logger logger = LoggerFactory.getLogger(NClient.class);
    /**
     * 实现注册路由
     */
    private ConcurrentMap<String, ClientManager> handlerConcurrentMap = Maps.newConcurrentMap();
    private List<Endpoint> endpoints = null;
    /**
     * 本地缓存地址列表
     */
    private Random random = new Random();
    private IRegistry registry;

    public NClient(IRegistry registry) {
        this.registry = registry;
        if (endpoints == null) {
            try {
                endpoints = registry.find("com.alibaba.dubbo.performance.demo.provider.IHelloService");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        logger.info("end point size:{}", endpoints.size());
    }

    /**
     * 处理请求
     */
    public Integer call(String interfaceName, String method, String parameterTypesString, String parameter) {
        try {
            NRequest request = new NRequest();
            request.setInterfaceName(interfaceName);
            request.setMethodName(method);
            request.setParameterTypesString(parameterTypesString);
            request.setParameter(parameter);
            // 获取provider节点
            Endpoint endpoint = selectRandom();
            ClientManager manager = getHandler(endpoint);
            Channel channel = manager.getChannel();
            // 保存请求
            NFuture future = new NFuture();
            NRequestHolder.put(request.getRequestId(), future);
            channel.writeAndFlush(request);
            Object result = null;
            try {
                result = future.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
            String res = new String((byte[]) result);
            return Integer.valueOf(res);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 处理loadbalance: 更具响应时间作loadbalance
     * 随机实现
     */
    public Endpoint selectEndPoint(IRegistry registry, NRequest request) throws Exception {
        if (endpoints == null) {
            synchronized (NClient.class) {
                if (endpoints == null) {
                    endpoints = registry.find(request.getInterfaceName());
                }
            }
        }
        /**
         *  重新从小到大排序，并发会使用响应时间最小的节点
         */
        List<Endpoint> endpointList = endpoints.stream().sorted(Comparator.comparing(Endpoint::getLimit)).collect(Collectors.toList());
        return endpointList.get(0);
    }

    private void recordEndpoint(List<Endpoint> endpoints, Endpoint endpoint) {
        if (null != endpoints) {
            endpoints.parallelStream().forEach(
                    e -> {
                        if (e.equals(endpoint)) {
                            e.setLimit(endpoint.getLimit());
                        }
                    }
            );
        }
    }

    private Endpoint selectRandom() throws Exception {
        if (null == endpoints) {
            synchronized (NClient.class) {
                if (null == endpoints) {
                    endpoints = registry.find("com.alibaba.dubbo.performance.demo.provider.IHelloService");
                }
            }
        }
        return endpoints.get(random.nextInt(endpoints.size()));
    }

    private ClientManager getHandler(Endpoint endpoint) {
        String key = endpoint.toString();
        ClientManager manager = handlerConcurrentMap.get(key);
        if (manager == null) {
            manager = new ClientManager(endpoint.getHost(), endpoint.getPort());
            handlerConcurrentMap.put(key, manager);
        }
        return manager;
    }

    Channel getChannel(Endpoint endpoint) {
        EventLoopGroup group = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group);
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel channel) throws Exception {
                ChannelPipeline pipeline = channel.pipeline();
                pipeline.addLast(new NEncoder(NRequest.class));
                pipeline.addLast(new NDecoder(NResponse.class));
                pipeline.addLast(new ClientHandler());
            }
        });
        bootstrap.option(ChannelOption.TCP_NODELAY, true);
        // 连接 RPC 服务器
        ChannelFuture future = null;
        try {
            return bootstrap.connect(endpoint.getHost(), endpoint.getPort()).sync().channel();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }
}
