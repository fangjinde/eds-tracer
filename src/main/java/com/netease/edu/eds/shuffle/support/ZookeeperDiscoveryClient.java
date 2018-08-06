package com.netease.edu.eds.shuffle.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import java.util.List;

/**
 * @author hzfjd
 * @create 18/8/6
 **/
public class ZookeeperDiscoveryClient implements DiscoveryClient {


    @Autowired
    org.apache.curator.x.discovery.ServiceInstance serviceInstance;

    @Override
    public String description() {
        return "ZookeeperDiscoveryClient";
    }

    @Override
    public ServiceInstance getLocalServiceInstance() {
        return null;
    }

    @Override
    public List<ServiceInstance> getInstances(String serviceId) {
        return null;
    }

    @Override
    public List<String> getServices() {
        return null;
    }
}
