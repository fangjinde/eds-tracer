package com.netease.edu.eds.trace.configuration;

/**
 * Created by hzfjd on 18/3/1.
 */

import brave.Tracing;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.sleuth.autoconfig.TraceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.netease.edu.eds.trace.instrument.elasticsearch.ElasticsearchTracing;

@Configuration
@ConditionalOnProperty(name = "spring.sleuth.elasticsearch.enabled", havingValue = "true", matchIfMissing = true)
@AutoConfigureAfter(TraceAutoConfiguration.class)
public class TraceElasticsearchAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ElasticsearchTracing elasticsearchTracing(Tracing tracing) {
        return ElasticsearchTracing.newBuilder().tracing(tracing).build();
    }
}
