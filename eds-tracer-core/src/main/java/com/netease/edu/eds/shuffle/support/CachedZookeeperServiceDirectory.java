package com.netease.edu.eds.shuffle.support;

import com.netease.edu.eds.shuffle.core.ServiceDirectory;
import org.apache.curator.x.discovery.ServiceCache;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * spring cloud 2.0版本的DiscoveryClient实现是没有本地缓存的。并且也不建议在生产环境使用zookeeper版本的服务发现去替换eureka。所以再单独实现了缓存版本的zookeeper服务发现。
 * 
 * @author hzfjd
 * @create 18/8/6
 **/
public class CachedZookeeperServiceDirectory implements ServiceDirectory {

    private static final Logger                 logger                    = LoggerFactory.getLogger(CachedZookeeperServiceDirectory.class);

    @Autowired
    private ServiceDiscovery<ServiceInstance>   serviceDiscovery;

    private ConcurrentMap<String, ServiceCache> serviceCacheConcurrentMap = new ConcurrentHashMap<>();

    @Override
    public Collection<String> queryForNames() {
        try {
            return serviceDiscovery.queryForNames();
        } catch (Exception e) {
            logger.error("queryForNames error", e);
            return Collections.EMPTY_LIST;
        }
    }

    @Override
    public Collection<ServiceInstance> queryForInstances(String name) {

        ServiceCache serviceCache = getServiceCacheByName(name);
        if (serviceCache == null) {
            return Collections.EMPTY_LIST;
        }
        return serviceCache.getInstances();

    }

    @Override
    public ServiceInstance queryForInstance(String name, String id) {

        Collection<ServiceInstance> instances = queryForInstances(name);
        ServiceInstance locatedInstance = null;
        for (ServiceInstance serviceInstance : instances) {
            if (serviceInstance.getId() != null && serviceInstance.getId().equals(id)) {
                locatedInstance = serviceInstance;
            }
        }

        return locatedInstance;
    }

    private ServiceCache getServiceCacheByName(String name) {
        ServiceCache serviceCache = serviceCacheConcurrentMap.get(name);
        if (serviceCache != null) {
            return serviceCache;
        }

        synchronized (this) {
            if (serviceCache != null) {
                return serviceCache;
            }
            serviceCache = serviceDiscovery.serviceCacheBuilder().name(name).build();
            try {
                serviceCache.start();
            } catch (Exception e) {
                logger.error("start serviceCache Fail", e);
                return null;
            }
            ServiceCache previous = serviceCacheConcurrentMap.putIfAbsent(name, serviceCache);
            return previous == null ? serviceCache : previous;
        }
    }
}
