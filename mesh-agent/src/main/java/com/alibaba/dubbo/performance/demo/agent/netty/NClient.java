package com.alibaba.dubbo.performance.demo.agent.netty;

import com.alibaba.dubbo.performance.demo.agent.registry.Endpoint;
import com.alibaba.dubbo.performance.demo.agent.registry.IRegistry;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
    private ExecutorService sendService = Executors.newFixedThreadPool(200);

    private ConcurrentMap<String, ClientManager> handlerConcurrentMap = new ConcurrentHashMap<>(16);

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
        logger.info("CA end point size:{}", endpoints.size());
        // 客户端预热
        if (endpoints.size() > 0) {
            for (Endpoint p : endpoints) {
                String key = p.getHost();
                if (!handlerConcurrentMap.containsKey(key)) {
                    handlerConcurrentMap.put(key, new ClientManager(p.getHost(), p.getPort()));
                }
            }
        }
    }

    /**
     * 处理请求-local-test-ok
     */
    public Integer call(String interfaceName, String method, String parameterTypesString, String parameter) {
        try {
            NRequest request = new NRequest();
            request.setInterfaceName(interfaceName);
            request.setMethodName(method);
            request.setParameterTypesString(parameterTypesString);
            request.setParameter(parameter);

            NFuture future = new NFuture();
            NRequestHolder.put(request.getRequestId(), future);
            long start = System.currentTimeMillis();

            Endpoint endpoint = selectRandom();
            ClientManager manager = getHandler(endpoint);
            Channel channel = manager.getChannel();
            logger.info("CA1:{}:{}", request.getRequestId(), System.currentTimeMillis() - start);
            // 保存请求-阻塞地点1
            channel.writeAndFlush(request);
            logger.info("CA2:{}:{}", request.getRequestId(), System.currentTimeMillis() - start);

            NResponse response = null;
            try {
                response = future.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
            logger.info("CA3:{}:{}", response.getRequestId(), System.currentTimeMillis() - start);
            String res = new String((byte[]) response.getResult());
            return Integer.valueOf(res);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 可以选用dubbo随机优化算法,此处简化
     */
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
        String key = endpoint.getHost();
        ClientManager manager = handlerConcurrentMap.get(key);
        if (manager == null) {
            manager = new ClientManager(endpoint.getHost(), endpoint.getPort());
            handlerConcurrentMap.put(key, manager);
        }
        return manager;
    }
}
