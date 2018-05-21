package com.alibaba.dubbo.performance.demo.agent.registry;

public class Endpoint {

    private final long limit;
    private final String host;
    private final int port;

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

    public String toString() {
        return host + ":" + port;
    }

    public boolean equals(Object o) {
        if (!(o instanceof Endpoint)) {
            return false;
        }
        Endpoint other = (Endpoint) o;
        return other.host.equals(this.host) && other.port == this.port;
    }

    public long getLimit() {
        return limit;
    }

    public int hashCode() {
        return host.hashCode() + port;
    }
}
