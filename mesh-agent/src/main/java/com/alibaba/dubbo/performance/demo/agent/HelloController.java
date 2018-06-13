package com.alibaba.dubbo.performance.demo.agent;

import com.alibaba.dubbo.performance.demo.agent.dubbo.RpcClient;
import com.alibaba.dubbo.performance.demo.agent.netty.NClient;
import com.alibaba.dubbo.performance.demo.agent.registry.EtcdRegistry;
import com.alibaba.dubbo.performance.demo.agent.registry.IRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author a002
 */
@RestController
public class HelloController {


    private Logger logger = LoggerFactory.getLogger(HelloController.class);
    private RpcClient rpcClient = new RpcClient();
    private IRegistry registry = new EtcdRegistry(System.getProperty("etcd.url"));
    private NClient nClient = new NClient(registry);
    private List<NClient> clientList = new ArrayList<>(100);
    private Random random = new Random();

    private ExecutorService service = Executors.newFixedThreadPool(100);

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
     * 需要增加异步
     */
    public Integer consumer(String interfaceName, String method, String parameterTypesString, String parameter) throws Exception {

        long start = System.currentTimeMillis();
        Future<Integer> res = service.submit(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return clientList.get(random.nextInt(10)).call(interfaceName, method, parameterTypesString, parameter);
            }
        });
        logger.info("C:{}", System.currentTimeMillis() - start);
        return res.get();
    }

    @PostConstruct
    public void init() {
        String type = System.getProperty("type");
        if ("consumer".equals(type)) {
            for (int i = 0; i < 10; i++) {
                NClient client = new NClient(registry);
                clientList.add(client);
            }
        }
    }

    public NClient getClient() {
        int index = random.nextInt(10);
        logger.info("index = {}", index);
        return clientList.get(index);
    }
}
