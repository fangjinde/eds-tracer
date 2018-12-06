package com.netease.edu.eds.trace.configuration;/**
                                                 * Created by hzfjd on 18/3/1.
                                                 */

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.netease.edu.eds.trace.constants.PropagationConstants;
import com.netease.edu.eds.trace.constants.TraceBeanNameConstants;
import com.netease.edu.eds.trace.properties.RedisProperties;
import com.netease.edu.eds.trace.properties.TraceProperties;
import com.netease.edu.eds.trace.support.EduExceptionMessageErrorParser;
import com.netease.edu.eds.trace.support.SpringBeanFactorySupport;
import com.netease.edu.eds.trace.support.TraceRedisSupport;
import com.netease.edu.eds.trace.utils.TraceContextPropagationUtils;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.sleuth.ErrorParser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * @author hzfjd
 * @create 18/3/1
 */
@Configuration
@EnableConfigurationProperties({ KafkaProperties.class })
@ConditionalOnProperty(value = "trace.enabled", matchIfMissing = true)
@AutoConfigureBefore(EduZipkinAutoConfiguration.class)
public class TraceBaseAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ErrorParser eduExceptionMessageErrorParser() {
        return new EduExceptionMessageErrorParser();
    }

    @Bean
    @ConditionalOnMissingBean
    public static SpringBeanFactorySupport springBeanFactorySupport() {
        return new SpringBeanFactorySupport();
    }

    @Bean
    @ConditionalOnMissingBean
    public static TraceProperties traceProperties() {
        return new TraceProperties();
    }

    @Bean
    @ConditionalOnMissingBean(name = TraceBeanNameConstants.TRACE_REDIS_CONNECTION_FACTORY_BEAN_NAME)
    public JedisConnectionFactory traceRedisConnectionFactory() {
        JedisConnectionFactory jedisConnectionFactory = new JedisConnectionFactory();
        RedisProperties redisProperties = traceRedisProperties();
        jedisConnectionFactory.setHostName(redisProperties.getHost());
        jedisConnectionFactory.setPassword(redisProperties.getPassword());
        jedisConnectionFactory.setPort(redisProperties.getPort());
        jedisConnectionFactory.setTimeout(redisProperties.getTimeout());
        return jedisConnectionFactory;
    }

    @Bean
    @ConditionalOnMissingBean(name = TraceBeanNameConstants.TRACE_REDIS_TEMPLATE_BEAN_NAME)
    public RedisOperations traceRedisTemplate() {
        RedisTemplate redisTemplate = new RedisTemplate();
        redisTemplate.setConnectionFactory(traceRedisConnectionFactory());
        return redisTemplate;
    }

    @Bean
    @ConfigurationProperties(prefix = "trace.redis")
    public RedisProperties traceRedisProperties() {
        return new RedisProperties();
    }

    /***
     * refresh whenever environment is changed will be overhead， later i will work it out.
     * 
     * @param traceProperties
     * @return
     */
    @Bean
    // @RefreshScope
    public LoadingCache<String, Object> traceContextCache(TraceProperties traceProperties) {
        CacheLoader<String, Object> cacheLoader = new CacheLoader<String, Object>() {

            @Override
            public Object load(String key) throws Exception {

                Object value = TraceRedisSupport.unsafeGet(key);

                if (!(value instanceof String)) {
                    return PropagationConstants.NULL_OBJECT;
                }

                value = TraceContextPropagationUtils.parseTraceContextFromJsonString((String) value);

                if (value == null) {
                    return PropagationConstants.NULL_OBJECT;
                }

                return value;
            }
        };
        LoadingCache<String, Object> loadingCache = CacheBuilder.newBuilder().maximumSize(traceProperties.getCache().getMaximumSize()).expireAfterAccess(traceProperties.getCache().getExpireAfterAccess(),
                                                                                                                                                         TimeUnit.MILLISECONDS).build(cacheLoader);
        return loadingCache;
    }
}
