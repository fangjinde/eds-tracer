package com.netease.edu.eds.trace.configuration;/**
 * Created by hzfjd on 18/3/1.
 */

import brave.Tracing;
import com.netease.edu.eds.trace.instrument.dubbo.DubboTracing;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.sleuth.autoconfig.TraceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author hzfjd
 * @create 18/3/1
 */
@Configuration
@ConditionalOnProperty(name = "spring.sleuth.dubbo.enabled", havingValue = "true", matchIfMissing = true)
@AutoConfigureAfter(TraceAutoConfiguration.class)
public class TraceDubboAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public DubboTracing dubboTracing(Tracing tracing) {
        return DubboTracing.create(tracing);
    }
}
