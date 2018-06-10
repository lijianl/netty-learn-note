package com.alibaba.dubbo.performance.demo.agent.netty;

import java.util.concurrent.*;


public class NFuture implements Future<NResponse> {

    private CountDownLatch latch = new CountDownLatch(1);
    private NResponse response;

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public NResponse get() throws InterruptedException, ExecutionException {
        latch.await();
        return response;
    }

    @Override
    public NResponse get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        boolean b = latch.await(timeout, unit);
        return response;
    }

    /**
     * 异步完成
     */
    public void done(NResponse response) {
        this.response = response;
        latch.countDown();
    }
}
