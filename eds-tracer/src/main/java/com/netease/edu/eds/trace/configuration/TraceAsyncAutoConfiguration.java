package com.netease.edu.eds.trace.configuration;/**
                                                 * Created by hzfjd on 18/3/22.
                                                 */

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.sleuth.autoconfig.TraceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.netease.edu.eds.trace.instrument.async.AsyncTracing;

import brave.Tracing;

/**
 * @author hzfjd
 * @create 18/3/22
 */
@Configuration
@ConditionalOnProperty(name = "spring.sleuth.async.enabled", havingValue = "true", matchIfMissing = true)
@AutoConfigureAfter(TraceAutoConfiguration.class)
public class TraceAsyncAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AsyncTracing asyncTracing(Tracing tracing) {
        return AsyncTracing.newBuilder(tracing).build();
    }

}
