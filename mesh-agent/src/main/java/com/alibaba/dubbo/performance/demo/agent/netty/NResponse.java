package com.alibaba.dubbo.performance.demo.agent.netty;

public class NResponse {

    private String requestId;
    private Object result;

    public String getRequestId() {
        return requestId;
    }
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }
}
