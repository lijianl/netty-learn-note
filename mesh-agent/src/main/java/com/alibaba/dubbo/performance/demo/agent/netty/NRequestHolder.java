package com.alibaba.dubbo.performance.demo.agent.netty;

import java.util.concurrent.ConcurrentHashMap;

public class NRequestHolder {

    private static ConcurrentHashMap<String, NFuture> processingRpc = new ConcurrentHashMap<>();

    public static void put(String requestId, NFuture rpcFuture) {
        processingRpc.put(requestId, rpcFuture);
    }

    public static NFuture get(String requestId) {
        return processingRpc.get(requestId);
    }

    public static void remove(String requestId) {
        processingRpc.remove(requestId);
    }
}
