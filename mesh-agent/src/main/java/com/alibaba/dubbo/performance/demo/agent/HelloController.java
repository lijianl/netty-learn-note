package com.alibaba.dubbo.performance.demo.agent;

import com.alibaba.dubbo.performance.demo.agent.dubbo.RpcClient;
import com.alibaba.dubbo.performance.demo.agent.netty.NClient;
import com.alibaba.dubbo.performance.demo.agent.registry.Endpoint;
import com.alibaba.dubbo.performance.demo.agent.registry.EtcdRegistry;
import com.alibaba.dubbo.performance.demo.agent.registry.IRegistry;
import okhttp3.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class HelloController {

    private RpcClient rpcClient = new RpcClient();
    private IRegistry registry = new EtcdRegistry(rpcClient, System.getProperty("etcd.url"));

    private List<Endpoint> endpoints = null;
    private Object lock = new Object();
    private OkHttpClient httpClient = new OkHttpClient();


    @RequestMapping(value = "")
    public Object invoke(@RequestParam("interface") String interfaceName,
                         @RequestParam("method") String method,
                         @RequestParam("parameterTypesString") String parameterTypesString,
                         @RequestParam("parameter") String parameter) throws Exception {
        String type = System.getProperty("type");   // 获取type参数
        if ("consumer".equals(type)) {
            return consumer(interfaceName, method, parameterTypesString, parameter);
        } else if ("provider".equals(type)) {
            return provider(interfaceName, method, parameterTypesString, parameter);
        } else {
            return "Environment variable type is needed to set to provider or consumer.";
        }
    }

    public byte[] provider(String interfaceName, String method, String parameterTypesString, String parameter) throws Exception {
        Object result = rpcClient.invoke(interfaceName, method, parameterTypesString, parameter);
        return (byte[]) result;
    }

    /**
     * 修改使用RPC
     */
    public Integer consumer(String interfaceName, String method, String parameterTypesString, String parameter) throws Exception {


        /**
         * 修改负载: 1. 使用netty 线程池代替http,
         *
         */
        //Endpoint endpoint = selectEndPoint(registry);

        /**
         * 不能缓存结果 && 修改协议
         */
        //String url = "http://" + endpoint.getHost() + ":" + endpoint.getPort();

        /*RequestBody requestBody = new FormBody.Builder()
                .add("interface", interfaceName)
                .add("method", method)
                .add("parameterTypesString", parameterTypesString)
                .add("parameter", parameter)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        long start = System.currentTimeMillis();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
            long limit = System.currentTimeMillis() - start;
            endpoint.setLimit(limit);
            recordEndpoint(endpoints, endpoint);
            byte[] bytes = response.body().bytes();
            String s = new String(bytes);
            return Integer.valueOf(s);
        }
*/
        /**
         * 通过netty使用tcp
         */
        NClient client = new NClient(registry);
        return client.call(interfaceName, method, parameterTypesString, parameter);
    }

    private Endpoint selectEndPoint(IRegistry registry) throws Exception {
        if (null == endpoints) {
            synchronized (lock) {
                if (null == endpoints) {
                    endpoints = registry.find("com.alibaba.dubbo.performance.demo.provider.IHelloService");
                }
            }
        }
        List<Endpoint> endpointList = endpoints.stream().sorted(Comparator.comparing(Endpoint::getLimit)).collect(Collectors.toList());
        return endpointList.get(0);
    }

    private void recordEndpoint(List<Endpoint> endpoints, Endpoint endpoint) {
        if (null != endpoints) {
            endpoints.forEach(
                    e -> {
                        if (e.equals(endpoint)) {
                            e.setLimit(endpoint.getLimit());
                        }
                    }
            );
        }
    }
}
