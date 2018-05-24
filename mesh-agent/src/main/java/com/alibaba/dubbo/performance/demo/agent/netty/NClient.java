package com.alibaba.dubbo.performance.demo.agent.netty;


import com.alibaba.dubbo.performance.demo.agent.registry.Endpoint;
import com.alibaba.dubbo.performance.demo.agent.registry.IRegistry;

import java.util.*;
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
    /**
     * 本地缓存地址列表
     */
    private List<Endpoint> endpoints = null;

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
            ClientHandler clientHandler = new ClientHandler(endpoint.getHost(), endpoint.getPort());
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

    public static void main(String[] args) {
        Random random = new Random();
        for (int i = 0; i < 10; i++) {
            System.out.println("args = [" + random.nextInt(3) + "]");
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
        return endpoints.get(2);
    }
}
