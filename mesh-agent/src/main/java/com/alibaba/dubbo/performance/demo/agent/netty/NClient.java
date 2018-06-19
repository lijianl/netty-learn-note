package com.alibaba.dubbo.performance.demo.agent.netty;

import com.alibaba.dubbo.performance.demo.agent.registry.Endpoint;
import com.alibaba.dubbo.performance.demo.agent.registry.IRegistry;
import io.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * consumer-agent 代理启动
 * 封装请求, 封装loadbalance
 */
public class NClient {

    private Logger logger = LoggerFactory.getLogger(NClient.class);

    private ConcurrentMap<String, ClientManager> handlerConcurrentMap = new ConcurrentHashMap<>();
    private List<Endpoint> endpoints = null;
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
        logger.info("PA end point size:{}", endpoints.size());
        // 客户端预热: 测试程序有预热运行
        /*if (endpoints.size() > 0) {
            for (Endpoint p : endpoints) {
                String key = p.getHost();
                if (!handlerConcurrentMap.containsKey(key)) {
                    handlerConcurrentMap.put(key, new ClientManager(p.getHost(), p.getPort()));
                }
            }
        }*/
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
            Endpoint endpoint = selectRandom();
            ClientManager manager = getHandler(endpoint);
            Channel channel = manager.getChannel();

            long start = System.currentTimeMillis();
            logger.info("CA1:{}:{}", request.getRequestId(), start);
            // 保存请求-阻塞地点1
            channel.writeAndFlush(request);
            logger.info("CA2:{}:{}:{}", request.getRequestId(), System.currentTimeMillis() - start, System.currentTimeMillis());
            NResponse response = null;
            try {
                response = future.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
            long end = System.currentTimeMillis();
            logger.info("CA3:{}:{}:{}", response.getRequestId(), end - start, end);
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
