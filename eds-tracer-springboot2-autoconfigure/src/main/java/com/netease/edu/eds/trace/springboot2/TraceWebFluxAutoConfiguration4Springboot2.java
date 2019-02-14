package com.netease.edu.eds.trace.springboot2;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.cloud.sleuth.instrument.web.TraceWebAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.netease.edu.eds.trace.springboot2.instrument.http.TraceWebFilter;
import com.netease.edu.eds.trace.springbootcompatible.spi.SkipUriMatcher;

import brave.Tracing;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration Auto-configuration} enables tracing to HTTP
 * requests with Spring WebFlux.
 *
 * @author Marcin Grzejszczak
 * @author hzfjd@author hzfjd
 * @since 2.0.0
 */
@Configuration
@ConditionalOnProperty(value = "spring.sleuth.web.enabled", matchIfMissing = true)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnBean(Tracing.class)
@AutoConfigureAfter(TraceWebAutoConfiguration.class)
public class TraceWebFluxAutoConfiguration4Springboot2 {

    @Bean
    @ConditionalOnMissingBean
    public TraceWebFilter traceWebFilter(BeanFactory beanFactory, ObjectProvider<SkipUriMatcher> skipUriMatcherOp) {
        return TraceWebFilter.create(beanFactory, skipUriMatcherOp.getIfAvailable());
    }
}
