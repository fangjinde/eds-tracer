package com.netease.edu.eds.shuffle.support;

import com.netease.edu.eds.shuffle.spi.EnvironmentDetector;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import java.util.List;

/**
 * @author hzfjd
 * @create 18/8/3
 **/
public class EnvironmentDetectorDiscoveryClientImpl implements EnvironmentDetector {

    @Autowired
    DiscoveryClient discoveryClient;

    @Override
    public boolean exist(String environmentName, String applicationName) {

        List<ServiceInstance> serviceInstances = discoveryClient.getInstances(applicationName + "." + environmentName);
        return CollectionUtils.isNotEmpty(serviceInstances);
    }
}
