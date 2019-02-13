package com.netease.edu.eds.trace.configuration;/**
                                                 * Created by hzfjd on 18/4/13.
                                                 */

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.sleuth.autoconfig.TraceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.netease.edu.eds.trace.instrument.rabbit.RabbitTracing;

import brave.Tracing;

/**
 * @author hzfjd
 * @create 18/4/13
 */
@Configuration
@ConditionalOnProperty(name = "spring.sleuth.rabbit.enabled", havingValue = "true", matchIfMissing = true)
@AutoConfigureAfter(TraceAutoConfiguration.class)
public class EduTraceRabbitAutoconfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RabbitTracing rabbitTracing(Tracing tracing) {
        return RabbitTracing.newBuilder().tracing(tracing).build();
    }

}
