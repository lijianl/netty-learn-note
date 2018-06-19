package com.alibaba.dubbo.performance.demo.agent;

import com.alibaba.dubbo.performance.demo.agent.netty.NClient;
import com.alibaba.dubbo.performance.demo.agent.registry.EtcdRegistry;
import com.alibaba.dubbo.performance.demo.agent.registry.IRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
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
    //private RpcClient rpcClient = new RpcClient();

    private IRegistry registry = new EtcdRegistry(System.getProperty("etcd.url"));
    private NClient nClient = null;
    private ExecutorService service = null;

    @RequestMapping(value = "")
    public Object invoke(@RequestParam("interface") String interfaceName,
                         @RequestParam("method") String method,
                         @RequestParam("parameterTypesString") String parameterTypesString,
                         @RequestParam("parameter") String parameter) throws Exception {
        String type = System.getProperty("type");
        if ("consumer".equals(type)) {

            return consumer(interfaceName, method, parameterTypesString, parameter);

        } else if ("provider".equals(type)) {
            //return provider(interfaceName, method, parameterTypesString, parameter);
            return "Environment provider type has changed to Netty Client.";
        } else {
            return "Environment variable type is needed to set to provider or consumer.";
        }
    }

    public byte[] provider(String interfaceName, String method, String parameterTypesString, String parameter) throws Exception {
        //Object result = rpcClient.invoke(interfaceName, method, parameterTypesString, parameter);
        //return (byte[]) result;
        return null;
    }

    /**
     * web层已经使用了线程池，为什么此处还在使用?
     * demo代码此处也使用了
     */
    public Integer consumer(String interfaceName, String method, String parameterTypesString, String parameter) throws Exception {
        Future<Integer> res = service.submit(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return nClient.call(interfaceName, method, parameterTypesString, parameter);
            }
        });
        return res.get();
    }


    /**
     * 分开初始化减少环境资源占用
     */
    @PostConstruct
    public void init() {
        String type = System.getProperty("type");
        if ("consumer".equals(type)) {
            logger.info("init CA.");
            nClient = new NClient(registry);
            service = Executors.newFixedThreadPool(200);
        }
        if ("provider".equals(type)) {
            logger.info("init PA.");
        }
    }


}
