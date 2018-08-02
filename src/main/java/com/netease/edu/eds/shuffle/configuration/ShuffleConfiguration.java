package com.netease.edu.eds.shuffle.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.netease.edu.eds.shuffle.core.BeanNameConstants;
import com.netease.edu.eds.shuffle.core.ShuffleProperties;
import com.netease.edu.eds.shuffle.support.InterProcessMutexContext;

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

    @Bean(name=BeanNameConstants.QUEUE_CONSUMER_MUTEX_CONTEXT)
    public InterProcessMutexContext queueConsumerMutextContext() {
        return new InterProcessMutexContext();
    }

    // @ConditionalOnProperty(value = "edu.service.shuffle.turnOn", havingValue = "true", matchIfMissing = false)
}
