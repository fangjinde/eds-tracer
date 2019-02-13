package com.netease.edu.eds.shuffle.support;

import com.netease.edu.eds.shuffle.core.ServiceDirectory;
import com.netease.edu.eds.shuffle.spi.EnvironmentDetector;
import org.apache.commons.collections.CollectionUtils;
import org.apache.curator.x.discovery.ServiceInstance;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;

/**
 * @author hzfjd
 * @create 18/8/6
 **/
public class EnvironmentDetectorServiceDiscoveryImpl implements EnvironmentDetector {

    @Autowired
    ServiceDirectory serviceDirectory;

    @Override
    public boolean exist(String environmentName, String applicationName) {
        String appNameForServiceDiscovery = applicationName + "." + environmentName;
        Collection<ServiceInstance> serviceInstances = serviceDirectory.queryForInstances(appNameForServiceDiscovery);
        return CollectionUtils.isNotEmpty(serviceInstances);
    }
}
