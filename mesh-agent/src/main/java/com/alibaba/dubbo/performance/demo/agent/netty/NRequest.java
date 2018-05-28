package com.alibaba.dubbo.performance.demo.agent.netty;

import java.util.concurrent.atomic.AtomicLong;

public class NRequest {

    private static AtomicLong atomicLong = new AtomicLong();

    private long requestId;
    private String interfaceName;
    private String methodName;
    private String parameterTypesString;
    private String parameter;

    public NRequest() {
        requestId = atomicLong.getAndIncrement();
    }

    public long getRequestId() {
        return requestId;
    }

    public void setRequestId(long requestId) {
        this.requestId = requestId;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getParameterTypesString() {
        return parameterTypesString;
    }

    public void setParameterTypesString(String parameterTypesString) {
        this.parameterTypesString = parameterTypesString;
    }

    public String getParameter() {
        return parameter;
    }

    public void setParameter(String parameter) {
        this.parameter = parameter;
    }
}
