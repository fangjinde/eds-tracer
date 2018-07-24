package com.netease.edu.eds.shuffle.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.netease.edu.eds.shuffle.core.ShuffleProperties;

/**
 * @author hzfjd
 * @create 18/7/20
 **/
@Configuration
public class ShuffleConfiguration {

    @Bean
    // @ConditionalOnProperty(value = "edu.service.shuffle.turnOn", havingValue = "true", matchIfMissing = false)
    public ShuffleProperties shuffleProperties() {
        return new ShuffleProperties();
    }
}
