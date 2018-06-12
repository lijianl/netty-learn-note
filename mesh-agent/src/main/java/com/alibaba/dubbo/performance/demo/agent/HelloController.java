package com.alibaba.dubbo.performance.demo.agent;

import com.alibaba.dubbo.performance.demo.agent.dubbo.RpcClient;
import com.alibaba.dubbo.performance.demo.agent.netty.NClient;
import com.alibaba.dubbo.performance.demo.agent.registry.EtcdRegistry;
import com.alibaba.dubbo.performance.demo.agent.registry.IRegistry;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author a002
 */
@RestController
public class HelloController {

    private RpcClient rpcClient = new RpcClient();
    private IRegistry registry = new EtcdRegistry(System.getProperty("etcd.url"));
    private NClient nClient = new NClient(registry);
    private List<NClient> clientList = new ArrayList<>(100);
    private Random random = new Random();

    @RequestMapping(value = "")
    public Object invoke(@RequestParam("interface") String interfaceName,
                         @RequestParam("method") String method,
                         @RequestParam("parameterTypesString") String parameterTypesString,
                         @RequestParam("parameter") String parameter) throws Exception {
        String type = System.getProperty("type");
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
     * 此处channel并发
     */
    public Integer consumer(String interfaceName, String method, String parameterTypesString, String parameter) throws Exception {
        return getClient().call(interfaceName, method, parameterTypesString, parameter);
    }

    @PostConstruct
    public void init() {
        String type = System.getProperty("type");
        if ("consumer".equals(type)) {
            for (int i = 0; i < 100; i++) {
                NClient client = new NClient(registry);
                clientList.add(client);
            }
        }
    }

    public NClient getClient() {
        return clientList.get(random.nextInt(100));
    }
}
