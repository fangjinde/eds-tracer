package com.netease.edu.eds.shuffle.configuration;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.x.discovery.*;
import org.apache.curator.x.discovery.details.InstanceSerializer;
import org.apache.curator.x.discovery.details.JsonInstanceSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;

import com.netease.edu.eds.shuffle.core.BeanNameConstants;
import com.netease.edu.eds.shuffle.core.ServiceDirectory;
import com.netease.edu.eds.shuffle.core.ShuffleProperties;
import com.netease.edu.eds.shuffle.instrument.rabbit.ShuffleDelayQueueBBP;
import com.netease.edu.eds.shuffle.instrument.rabbit.ShuffleRouteBackLifeCycle;
import com.netease.edu.eds.shuffle.instrument.rabbit.SpringRabbitComponentNameEnvironmentCustomBeanFactoryPostProcessor;
import com.netease.edu.eds.shuffle.spi.EnvironmentDetector;
import com.netease.edu.eds.shuffle.spi.KeyValueManager;
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

    @Bean
    @ConditionalOnProperty(value = "shuffle.turnOn", havingValue = "true", matchIfMissing = false)
    public static SpringRabbitComponentNameEnvironmentCustomBeanFactoryPostProcessor springRabbitComponentNameEnvironmentCustomBeanFactoryPostProcessor() {
        return new SpringRabbitComponentNameEnvironmentCustomBeanFactoryPostProcessor();
    }

    @Bean
    @ConditionalOnProperty(value = "shuffle.turnOn", havingValue = "true", matchIfMissing = false)
    public static ShuffleDelayQueueBBP shuffleDelayQueueBBP() {
        return new ShuffleDelayQueueBBP();
    }

    @Bean
    @ConditionalOnProperty(value = "shuffle.turnOn", havingValue = "true", matchIfMissing = false)
    public ShuffleRouteBackLifeCycle shuffleRouteBackLifeCycle(){
        return new ShuffleRouteBackLifeCycle();
    }

    @Bean(name = BeanNameConstants.QUEUE_CONSUMER_MUTEX_CONTEXT)
    public InterProcessMutexContext queueConsumerMutextContext(CuratorFramework client) {
        return new InterProcessMutexContext(client, BeanNameConstants.QUEUE_CONSUMER_MUTEX_CONTEXT);
    }

    @Bean(name = BeanNameConstants.QUEUE_REDECLARE_MUTEX_CONTEXT)
    public InterProcessMutexContext queueRedeclareMutexContext(CuratorFramework client) {
        return new InterProcessMutexContext(client, BeanNameConstants.QUEUE_REDECLARE_MUTEX_CONTEXT);
    }

    @Bean(name = BeanNameConstants.ENVIRONMENT_DETECTOR)
    public EnvironmentDetector environmentDetector() {
        // return new EnvironmentDetectorServiceDiscoveryImpl();
        return new EnvironmentDetectorDiscoveryClientImpl();
    }

    @Bean
    @ConditionalOnMissingBean
    public InstanceSerializer<java.util.HashMap> deprecatedInstanceSerializer() {
        return new JsonInstanceSerializer(java.util.HashMap.class);
    }

    @Bean
    @ConditionalOnMissingBean
    public ZookeeperDiscoveryProperties zookeeperDiscoveryProperties(InetUtils inetUtils) {
        return new ZookeeperDiscoveryProperties(inetUtils);
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

        serviceInstanceBuilder.name(appNameForServiceDiscovery).address(host).payload(properties.getMetadata()).uriSpec(new UriSpec(properties.getUriSpec()));

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

    @Bean
    @ConditionalOnMissingBean(name = BeanNameConstants.SHUFFLE_REDIS_CONNECTION_FACTORY)
    public JedisConnectionFactory shuffleRedisConnectionFactory() {
        JedisConnectionFactory jedisConnectionFactory = new JedisConnectionFactory();
        RedisProperties redisProperties = shuffleRedisProperties();
        jedisConnectionFactory.setHostName(redisProperties.getHost());
        jedisConnectionFactory.setPassword(redisProperties.getPassword());
        jedisConnectionFactory.setPort(redisProperties.getPort());
        jedisConnectionFactory.setTimeout(redisProperties.getTimeout());
        return jedisConnectionFactory;
    }

    @Bean
    @ConditionalOnMissingBean(name = BeanNameConstants.SHUFFLE_REDIS_TEMPLATE)
    public RedisOperations shuffleRedisTemplate() {
        RedisTemplate redisTemplate = new RedisTemplate();
        redisTemplate.setConnectionFactory(shuffleRedisConnectionFactory());
        return redisTemplate;
    }

    @Bean
    @ConfigurationProperties(prefix = "shuffle.redis")
    public RedisProperties shuffleRedisProperties() {
        return new RedisProperties();
    }

    @Bean
    @ConditionalOnMissingBean(name = BeanNameConstants.SHUFFLE_REDIS_KEY_VALUE_MANAGER)
    public KeyValueManager shuffleRedisKeyValueManager() {
        return new RedisKeyValueManager();
    }

    // @ConditionalOnProperty(value = "edu.service.shuffle.turnOn", havingValue = "true", matchIfMissing = false)
}
