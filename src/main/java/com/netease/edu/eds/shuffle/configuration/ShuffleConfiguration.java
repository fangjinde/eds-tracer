package com.netease.edu.eds.shuffle.configuration;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.x.discovery.*;
import org.apache.curator.x.discovery.details.InstanceSerializer;
import org.apache.curator.x.discovery.details.JsonInstanceSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.zookeeper.discovery.ZookeeperInstance;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import com.netease.edu.eds.shuffle.core.BeanNameConstants;
import com.netease.edu.eds.shuffle.core.ServiceDirectory;
import com.netease.edu.eds.shuffle.core.ShuffleProperties;
import com.netease.edu.eds.shuffle.spi.EnvironmentDetector;
import com.netease.edu.eds.shuffle.support.*;

/**
 * @author hzfjd
 * @create 18/7/20
 **/
@Configuration
public class ShuffleConfiguration {

    @Bean
    public ShuffleProperties shuffleProperties() {
        return new ShuffleProperties();
    }

    @Bean(name = BeanNameConstants.QUEUE_CONSUMER_MUTEX_CONTEXT)
    public InterProcessMutexContext queueConsumerMutextContext() {
        return new InterProcessMutexContext();
    }

    @Bean(name = BeanNameConstants.ENVIRONMENT_DETECTOR)
    public EnvironmentDetector environmentDetector() {
        return new EnvironmentDetectorServiceDiscoveryImpl();
    }

    @Bean
    @ConditionalOnMissingBean
    public InstanceSerializer<ZookeeperInstance> deprecatedInstanceSerializer() {
        return new JsonInstanceSerializer<>(ZookeeperInstance.class);
    }

    @Bean
    @ConditionalOnMissingBean
    public ServiceInstance serviceInstance(ApplicationContext context, ZookeeperDiscoveryProperties properties) {
        String appName = context.getEnvironment().getProperty("spring.application.name", "application");
        String profile = context.getEnvironment().getProperty("spring.profiles.active", "default");
        String appNameForServiceDiscovery = appName + "." + profile;
        String host = properties.getInstanceHost();
        if (!StringUtils.hasText(host)) {
            throw new IllegalStateException("instanceHost must not be empty");
        }

        ServiceInstanceBuilder serviceInstanceBuilder = null;
        try {
            serviceInstanceBuilder = ServiceInstance.builder();
        } catch (Exception e) {
            throw new RuntimeException("create ServiceInstance.builder() failed. ", e);
        }

        serviceInstanceBuilder.id(context.getId()).name(appNameForServiceDiscovery).address(host).payload(properties.getMetadata()).uriSpec(new UriSpec(properties.getUriSpec()));

        if (properties.getInstanceSslPort() != null) {
            serviceInstanceBuilder.sslPort(properties.getInstanceSslPort());
        }
        if (properties.getInstanceId() != null) {
            serviceInstanceBuilder.id(properties.getInstanceId());
        }

        return serviceInstanceBuilder.build();
    }

    @Bean
    @ConditionalOnMissingBean
    public ServiceDiscovery<ServiceInstance> curatorServiceDiscovery(CuratorFramework curatorFramework,
                                                                     ZookeeperDiscoveryProperties properties,
                                                                     InstanceSerializer instanceSerializer,
                                                                     ServiceInstance serviceInstance) {

        return ServiceDiscoveryBuilder.builder(ServiceInstance.class).client(curatorFramework).basePath(properties.getRoot()).serializer(instanceSerializer).thisInstance(serviceInstance).watchInstances(true).build();
    }

    @Bean
    @ConditionalOnMissingBean
    public ServiceDiscoveryLifecycle serviceDiscoveryLifecycle() {
        return new ServiceDiscoveryLifecycle();
    }

    @Bean
    @ConditionalOnMissingBean
    public ServiceDirectory cachedZookeeperServiceDirectory() {
        return new CachedZookeeperServiceDirectory();
    }

    // @ConditionalOnProperty(value = "edu.service.shuffle.turnOn", havingValue = "true", matchIfMissing = false)
}
