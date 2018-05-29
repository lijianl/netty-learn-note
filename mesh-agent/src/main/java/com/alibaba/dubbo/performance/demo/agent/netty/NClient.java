package com.alibaba.dubbo.performance.demo.agent.netty;

import com.alibaba.dubbo.performance.demo.agent.registry.Endpoint;
import com.alibaba.dubbo.performance.demo.agent.registry.IRegistry;
import com.google.common.collect.Maps;
import io.netty.channel.Channel;

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
    private ConcurrentMap<String, ClientManager> handlerConcurrentMap = Maps.newConcurrentMap();
    private List<Endpoint> endpoints = null;
    /**
     * 本地缓存地址列表
     */
    private Random random = new Random();
    private IRegistry registry;

    public NClient(IRegistry registry) {
        this.registry = registry;
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
            NRequestHolder.put(String.valueOf(request.getRequestId()), future);
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
}
