package com.alibaba.dubbo.performance.demo.agent.netty;


import com.alibaba.dubbo.performance.demo.agent.registry.Endpoint;
import com.alibaba.dubbo.performance.demo.agent.registry.IRegistry;
import com.google.common.collect.Maps;

import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * consumer-agent 代理启动
 * 封装请求, 封装loadbalance
 */
public class NClient {

    /**
     * 实现注册路由
     */
    private IRegistry registry;
    private ConcurrentMap<String, ClientHandler> handlerConcurrentMap = Maps.newConcurrentMap();
    /**
     * 本地缓存地址列表
     */
    private List<Endpoint> endpoints = null;
    private Random random = new Random();

    public NClient(IRegistry registry) {
        this.registry = registry;
    }

    /**
     * 处理请求
     */
    public Integer call(String interfaceName, String method, String parameterTypesString, String parameter) {
        NRequest request = new NRequest();
        request.setRequestId(UUID.randomUUID().toString());
        request.setInterfaceName(interfaceName);
        request.setMethodName(method);
        request.setParameterTypesString(parameterTypesString);
        request.setParameter(parameter);
        try {
            Endpoint endpoint = selectRandom(registry);
            //Endpoint endpoint = selectEndPoint(registry, request);
            ClientHandler clientHandler = getHandler(endpoint);
//            long start = System.currentTimeMillis();
            NResponse response = clientHandler.send(request);
            String res = response.getResult().toString();
//            long weight = System.currentTimeMillis() - start;
//            endpoint.setLimit(weight);
//            recordEndpoint(endpoints, endpoint);
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

    private Endpoint selectRandom(IRegistry registry) throws Exception {
        if (null == endpoints) {
            synchronized (NClient.class) {
                if (null == endpoints) {
                    endpoints = registry.find("com.alibaba.dubbo.performance.demo.provider.IHelloService");
                }
            }
        }
        return endpoints.get(random.nextInt(endpoints.size()));
    }

    private ClientHandler getHandler(Endpoint endpoint) {
        String key = endpoint.toString();
        ClientHandler handler = handlerConcurrentMap.get(key);
        if (handler == null) {
            handler = new ClientHandler(endpoint.getHost(), endpoint.getPort());
            handlerConcurrentMap.put(key, handler);
        }
        return handler;
    }
}
