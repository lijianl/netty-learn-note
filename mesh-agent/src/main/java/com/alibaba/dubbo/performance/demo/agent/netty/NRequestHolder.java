package com.alibaba.dubbo.performance.demo.agent.netty;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author a002
 */
public class NRequestHolder {


    private static ConcurrentHashMap<Long, NFuture> processingRpc = new ConcurrentHashMap<>(1024);

    public static void put(Long requestId, NFuture rpcFuture) {
        processingRpc.put(requestId, rpcFuture);
    }

    public static NFuture get(Long requestId) {
        return processingRpc.get(requestId);
    }

    public static void remove(Long requestId) {
        processingRpc.remove(requestId);
    }
}
