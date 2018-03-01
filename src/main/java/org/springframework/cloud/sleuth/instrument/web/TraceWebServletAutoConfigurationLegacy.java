/*
 * Copyright 2013-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.sleuth.instrument.web;

import brave.Tracer;
import brave.http.HttpTracing;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.cloud.sleuth.ErrorParser;
import org.springframework.cloud.sleuth.SpanNamer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static javax.servlet.DispatcherType.*;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration
 * Auto-configuration} enables tracing to HTTP requests.
 *
 * @author Marcin Grzejszczak
 * @author Spencer Gibb
 * @since 1.0.0
 */
@Configuration
@ConditionalOnProperty(value = "spring.sleuth.web.enabled", matchIfMissing = true)
@ConditionalOnBean(HttpTracing.class)
@AutoConfigureAfter(TraceHttpAutoConfiguration.class)
public class TraceWebServletAutoConfigurationLegacy {

    /**
     * Nested config that configures Web MVC if it's present (without adding a runtime
     * dependency to it)
     */
    @Configuration
    @ConditionalOnClass(WebMvcConfigurer.class)
    @Import(TraceWebMvcConfigurerLegacy.class)
    protected static class TraceWebMvcAutoConfiguration {

    }

    @Bean TraceWebAspect traceWebAspect(Tracer tracer, SpanNamer spanNamer, ErrorParser errorParser) {
        return new TraceWebAspect(tracer, spanNamer, errorParser);
    }

    @Bean
    @ConditionalOnClass(name = "org.springframework.data.rest.webmvc.support.DelegatingHandlerMapping")
    public TraceSpringDataBeanPostProcessor traceSpringDataBeanPostProcessor(
            BeanFactory beanFactory) {
        return new TraceSpringDataBeanPostProcessor(beanFactory);
    }

    @Bean
    public FilterRegistrationBean traceWebFilter(
            TraceFilterLegacy traceFilter) {
        FilterRegistrationBean filterRegistrationBean = new FilterRegistrationBean(traceFilter);
        filterRegistrationBean.setDispatcherTypes(ASYNC, ERROR, FORWARD, INCLUDE, REQUEST);
        filterRegistrationBean.setOrder(TraceFilterLegacy.ORDER);
        return filterRegistrationBean;
    }

    @Bean
    @ConditionalOnMissingBean
    public TraceFilterLegacy traceFilter(BeanFactory beanFactory,
                                   SkipPatternProvider skipPatternProvider) {
        return new TraceFilterLegacy(beanFactory, skipPatternProvider.skipPattern());
    }
}

