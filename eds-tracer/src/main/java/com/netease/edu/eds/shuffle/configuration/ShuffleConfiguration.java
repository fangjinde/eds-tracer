package com.netease.edu.eds.shuffle.configuration;

import com.netease.edu.eds.shuffle.core.BeanNameConstants;
import com.netease.edu.eds.shuffle.core.ServiceDirectory;
import com.netease.edu.eds.shuffle.core.ShuffleProperties;
import com.netease.edu.eds.shuffle.instrument.rabbit.ShuffleDelayQueueBBP;
import com.netease.edu.eds.shuffle.instrument.rabbit.ShuffleRouteBackLifeCycle;
import com.netease.edu.eds.shuffle.instrument.rabbit.SpringRabbitComponentNameEnvironmentCustomBeanFactoryPostProcessor;
import com.netease.edu.eds.shuffle.spi.EnvironmentDetector;
import com.netease.edu.eds.shuffle.spi.KeyValueManager;
import com.netease.edu.eds.shuffle.support.*;
import com.netease.edu.eds.trace.properties.RedisProperties;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.x.discovery.*;
import org.apache.curator.x.discovery.details.InstanceSerializer;
import org.apache.curator.x.discovery.details.JsonInstanceSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import redis.clients.jedis.JedisPoolConfig;

import java.net.URI;
import java.net.URISyntaxException;

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

    @Configuration
    @ConditionalOnProperty(value = "shuffle.turnOn", havingValue = "true", matchIfMissing = false)
    public static class ShuffleComponentConfiguration {

        @Configuration
        @ConditionalOnProperty(value = "shuffle.turnOn", havingValue = "true", matchIfMissing = false)
        @ConditionalOnClass(name = "org.springframework.amqp.core.MessageListener")
        public static class RabbitShuffleConfiguration {

            @Bean
            public static SpringRabbitComponentNameEnvironmentCustomBeanFactoryPostProcessor springRabbitComponentNameEnvironmentCustomBeanFactoryPostProcessor() {
                return new SpringRabbitComponentNameEnvironmentCustomBeanFactoryPostProcessor();
            }

            @Bean
            public static ShuffleDelayQueueBBP shuffleDelayQueueBBP() {
                return new ShuffleDelayQueueBBP();
            }

            @Bean
            public ShuffleRouteBackLifeCycle shuffleRouteBackLifeCycle() {
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
        }

        @Configuration
        @ConditionalOnProperty(value = "shuffle.environment.detector", havingValue = "eureka", matchIfMissing = true)
        public static class EurekaEnvironmentDetectorConfiguration {

            @Bean(name = BeanNameConstants.ENVIRONMENT_DETECTOR)
            public EnvironmentDetector environmentDetector() {
                return new EnvironmentDetectorDiscoveryClientImpl();
            }
        }

        @Configuration
        @ConditionalOnProperty(value = "shuffle.environment.detector", havingValue = "zookeeper", matchIfMissing = false)
        public static class ZookeeperEnvironmentDetectorConfiguration {

            @Bean(name = BeanNameConstants.ENVIRONMENT_DETECTOR)
            public EnvironmentDetector environmentDetector() {
                return new EnvironmentDetectorServiceDiscoveryImpl();
            }

            @Bean
            @ConditionalOnMissingBean
            public ServiceDirectory cachedZookeeperServiceDirectory() {
                return new CachedZookeeperServiceDirectory();
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
            public ZookeeperDiscoveryProperties zookeeperDiscoveryProperties(InetUtils inetUtils) {
                return new ZookeeperDiscoveryProperties(inetUtils);
            }

            @Bean
            @ConditionalOnMissingBean
            public ServiceInstance serviceInstance(ApplicationContext context,
                                                   ZookeeperDiscoveryProperties properties) {
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
            public InstanceSerializer<java.util.HashMap> deprecatedInstanceSerializer() {
                return new JsonInstanceSerializer(java.util.HashMap.class);
            }

        }

        @Configuration
        @ConditionalOnProperty(value = "shuffle.turnOn", havingValue = "true", matchIfMissing = false)
        public static class ShuffleKeyValueManagerConfiguration {

            @Bean
            @ConditionalOnMissingBean(name = BeanNameConstants.SHUFFLE_REDIS_KEY_VALUE_MANAGER)
            public KeyValueManager shuffleRedisKeyValueManager() {
                return new RedisKeyValueManager();
            }

            @Bean
            @ConditionalOnMissingBean(name = BeanNameConstants.SHUFFLE_REDIS_TEMPLATE)
            public RedisOperations shuffleRedisTemplate() {
                RedisTemplate redisTemplate = new RedisTemplate();
                redisTemplate.setConnectionFactory(shuffleRedisConnectionFactory());
                return redisTemplate;
            }

            @Bean
            @ConditionalOnMissingBean(RedisConnectionFactory.class)
            public JedisConnectionFactory shuffleRedisConnectionFactory() {
                return applyProperties(createJedisConnectionFactory());
            }

            @Bean
            @ConfigurationProperties(prefix = "shuffle.redis")
            public RedisProperties shuffleRedisProperties() {
                return new RedisProperties();
            }

            private JedisConnectionFactory createJedisConnectionFactory() {
                JedisPoolConfig poolConfig = shuffleRedisProperties().getPool() != null ? jedisPoolConfig() : new JedisPoolConfig();
                return new JedisConnectionFactory(poolConfig);
            }

            private JedisPoolConfig jedisPoolConfig() {
                JedisPoolConfig config = new JedisPoolConfig();
                RedisProperties.Pool props = shuffleRedisProperties().getPool();
                config.setMaxTotal(props.getMaxActive());
                config.setMaxIdle(props.getMaxIdle());
                config.setMinIdle(props.getMinIdle());
                config.setMaxWaitMillis(props.getMaxWait());
                return config;
            }

            private JedisConnectionFactory applyProperties(JedisConnectionFactory factory) {
                configureConnection(factory);
                if (shuffleRedisProperties().isSsl()) {
                    factory.setUseSsl(true);
                }
                factory.setDatabase(shuffleRedisProperties().getDatabase());
                if (shuffleRedisProperties().getTimeout() > 0) {
                    factory.setTimeout(shuffleRedisProperties().getTimeout());
                }
                return factory;
            }

            private void configureConnection(JedisConnectionFactory factory) {
                if (StringUtils.hasText(shuffleRedisProperties().getUrl())) {
                    configureConnectionFromUrl(factory);
                } else {
                    factory.setHostName(shuffleRedisProperties().getHost());
                    factory.setPort(shuffleRedisProperties().getPort());
                    if (shuffleRedisProperties().getPassword() != null) {
                        factory.setPassword(shuffleRedisProperties().getPassword());
                    }
                }
            }

            private void configureConnectionFromUrl(JedisConnectionFactory factory) {
                String url = shuffleRedisProperties().getUrl();
                if (url.startsWith("rediss://")) {
                    factory.setUseSsl(true);
                }
                try {
                    URI uri = new URI(url);
                    factory.setHostName(uri.getHost());
                    factory.setPort(uri.getPort());
                    if (uri.getUserInfo() != null) {
                        String password = uri.getUserInfo();
                        int index = password.indexOf(":");
                        if (index >= 0) {
                            password = password.substring(index + 1);
                        }
                        factory.setPassword(password);
                    }
                } catch (URISyntaxException ex) {
                    throw new IllegalArgumentException("Malformed 'spring.redis.url' " + url, ex);
                }
            }

        }

    }

}
