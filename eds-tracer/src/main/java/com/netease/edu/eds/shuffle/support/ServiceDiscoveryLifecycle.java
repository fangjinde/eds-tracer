package com.netease.edu.eds.shuffle.support;

import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author hzfjd
 * @create 18/8/6
 **/
public class ServiceDiscoveryLifecycle implements SmartLifecycle {

    @Autowired
    ServiceDiscovery             serviceDiscovery;
    @Autowired
    ZookeeperDiscoveryProperties properties;
    @Autowired
    ServiceInstance              serviceInstance;

    AtomicBoolean                running = new AtomicBoolean(false);

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public void stop(Runnable callback) {
        doStop(callback);
    }

    private void doStop(Runnable callback) {
        try {
            if (!properties.isRegister()) {
                return;
            }
            serviceDiscovery.unregisterService(serviceInstance);
            running.compareAndSet(true, false);
        } catch (Exception e) {
            throw new RuntimeException("fail to unregisterService.", e);
        }
    }

    @Override
    public void start() {
        try {
            if (!properties.isRegister()) {
                return;
            }
            serviceDiscovery.start();
            running.compareAndSet(false, true);
        } catch (Exception e) {
            throw new RuntimeException("fail to register service.", e);
        }
    }

    @Override
    public void stop() {
        doStop(null);
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE - 1;
    }
}
