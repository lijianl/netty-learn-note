package com.alibaba.dubbo.performance.demo.agent.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author a002
 * <p>
 * 不需要初始化channel, 有预热程序
 */
public class ClientManager {

    private Logger logger = LoggerFactory.getLogger(ClientManager.class);

    private NioEventLoopGroup nioEventLoopGroup = null;
    private Object lock = new Object();

    private String host;
    private Integer port;

    private Bootstrap bootstrap;
    private Channel channel;


    public ClientManager(String host, Integer port) {
        this.host = host;
        this.port = port;
    }

    public Channel getChannel() throws Exception {
        if (null != channel) {
            return channel;
        }

        if (null == bootstrap) {
            synchronized (lock) {
                if (null == bootstrap) {
                    initBootstrap();
                }
            }
        }

        if (null == channel) {
            synchronized (lock) {
                if (null == channel) {
                    logger.info("consumer connect to provider {}:{}", host, port);
                    channel = bootstrap.connect(host, port).sync().channel();
                }
            }
        }

        return channel;
    }

    /**
     * 保证方法的原子属性:函数式编程的误区
     */
    public void initBootstrap() {
        bootstrap = new Bootstrap();
        nioEventLoopGroup = new NioEventLoopGroup(4, new DefaultThreadFactory("NettyClientWorker", true));
        bootstrap.group(nioEventLoopGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel channel) throws Exception {
                        channel.pipeline()
                                .addLast(new NEncoder(NRequest.class))
                                .addLast(new NDecoder(NResponse.class))
                                .addLast(new ClientHandler());
                    }
                });
    }
}
