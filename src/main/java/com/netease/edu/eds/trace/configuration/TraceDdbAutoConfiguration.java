package com.netease.edu.eds.trace.configuration;/**
 * Created by hzfjd on 18/3/22.
 */

import brave.Tracing;
import com.netease.edu.eds.trace.instrument.ddb.DdbTraceBeanFactoryPostProcessor;
import com.netease.edu.eds.trace.instrument.ddb.DdbTraceBeanPostProcessor;
import com.netease.edu.eds.trace.instrument.ddb.DdbTracing;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.sleuth.autoconfig.TraceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author hzfjd
 * @create 18/3/22
 */
@Configuration
@ConditionalOnBean(Tracing.class)
@ConditionalOnProperty(name = "spring.sleuth.ddb.enabled", havingValue = "true", matchIfMissing = true)
@AutoConfigureAfter(TraceAutoConfiguration.class)
public class TraceDdbAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public DdbTracing ddbTracing(Tracing tracing) {
        return DdbTracing.newBuilder(tracing).build();
    }

    @Bean
    @ConditionalOnMissingBean
    public static DdbTraceBeanPostProcessor ddbTraceBeanPostProcessor() {
        return new DdbTraceBeanPostProcessor();
    }

    @Bean
    @ConditionalOnMissingBean
    public static DdbTraceBeanFactoryPostProcessor ddbTraceBeanFactoryPostProcessor() {
        return new DdbTraceBeanFactoryPostProcessor();
    }
}
