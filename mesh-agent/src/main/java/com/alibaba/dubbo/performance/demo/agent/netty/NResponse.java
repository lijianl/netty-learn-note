package com.alibaba.dubbo.performance.demo.agent.netty;

public class NResponse {

    private long requestId;

    private Object result;

    public long getRequestId() {
        return requestId;
    }

    public void setRequestId(long requestId) {
        this.requestId = requestId;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }
}
