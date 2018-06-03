package com.alibaba.dubbo.performance.demo.agent.registry;

public class Endpoint {

    /**
     * 权重设计
     */
    private long limit;
    private String host;
    private int port;

    public Endpoint(String host, int port) {
        this.host = host;
        this.port = port;
        limit = 0;
    }

    public Endpoint(long limit, String host, int port) {
        this.limit = limit;
        this.host = host;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return host + ":" + port;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Endpoint)) {
            return false;
        }
        Endpoint other = (Endpoint) o;
        return other.host.equals(this.host) && other.port == this.port;
    }

    @Override
    public int hashCode() {
        return host.hashCode() + port;
    }

    public long getLimit() {
        return limit;
    }

    public void setLimit(long limit) {
        this.limit = limit;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
