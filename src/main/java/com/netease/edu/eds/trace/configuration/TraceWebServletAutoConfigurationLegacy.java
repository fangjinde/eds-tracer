/*
 * Copyright 2013-2018 the original author or authors. Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the
 * License.
 */

package com.netease.edu.eds.trace.configuration;

import brave.http.HttpTracing;
import com.netease.edu.eds.trace.constants.BeanNameConstants;
import com.netease.edu.eds.trace.instrument.http.*;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.sleuth.instrument.web.TraceHttpAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static javax.servlet.DispatcherType.*;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration Auto-configuration} enables tracing to HTTP
 * requests.
 *
 * @author Marcin Grzejszczak
 * @author Spencer Gibb
 * @since 1.0.0
 */
@Configuration
@ConditionalOnProperty(value = "spring.sleuth.web.enabled", matchIfMissing = true)
@AutoConfigureAfter(TraceHttpAutoConfiguration.class)
public class TraceWebServletAutoConfigurationLegacy {

    /**
     * Nested config that configures Web MVC if it's present (without adding a runtime dependency to it)
     */
    @Configuration
    @ConditionalOnClass(WebMvcConfigurer.class)
    @Import(TraceWebMvcConfigurerLegacy.class)
    public static class TraceWebMvcAutoConfiguration {

    }

    @ConditionalOnClass(name = { "com.netease.edu.web.cookie.utils.NeteaseEduCookieManager",
                                 "com.netease.edu.web.utils.WebUser",
                                 "com.netease.edu.web.config.EduWebProjectConfig" })
    @Configuration
    public static class WebAppTraceConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public WebUserMatcher webUserMatcher() {
            return new EduWebUserMatcher();

        }
    }

    @Bean
    @ConditionalOnMissingBean
    @RefreshScope
    public WebDebugMatcher webDebugMatcher() {
        return new DefaultWebDebugMatcher();
    }

    @Bean
    @ConditionalOnMissingBean(name = BeanNameConstants.TRACE_FILTER)
    public FilterRegistrationBean traceFilter(HttpTracing httpTracing, SkipUriMatcher skipUriMatcher,
                                              WebDebugMatcher webDebugMatcher) {
        FilterRegistrationBean filterRegistrationBean = new FilterRegistrationBean(TracingFilter.create(httpTracing,
                                                                                                        skipUriMatcher,
                                                                                                        webDebugMatcher));
        filterRegistrationBean.setDispatcherTypes(ASYNC, ERROR, FORWARD, INCLUDE, REQUEST);
        filterRegistrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        return filterRegistrationBean;
    }

    // @Bean
    // public FilterRegistrationBean traceWebFilter(
    // TraceFilterLegacy traceFilter) {
    // FilterRegistrationBean filterRegistrationBean = new FilterRegistrationBean(traceFilter);
    // filterRegistrationBean.setDispatcherTypes(ASYNC, ERROR, FORWARD, INCLUDE, REQUEST);
    // filterRegistrationBean.setOrder(TraceFilterLegacy.ORDER);
    // return filterRegistrationBean;
    // }

    // @Bean
    // @ConditionalOnMissingBean
    // public TraceFilterLegacy traceFilter(BeanFactory beanFactory,
    // SkipPatternProvider skipPatternProvider) {
    // return new TraceFilterLegacy(beanFactory, skipPatternProvider.skipPattern());
    // }

    // TracingFilter
}
