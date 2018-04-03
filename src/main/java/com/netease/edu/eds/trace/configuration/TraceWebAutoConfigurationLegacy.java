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
package com.netease.edu.eds.trace.configuration;

import brave.Tracing;
import brave.http.HttpTracing;
import com.netease.edu.eds.trace.instrument.http.HttpServerParserCustomed;
import com.netease.edu.eds.trace.instrument.http.SkipUriMatcher;
import com.netease.edu.eds.trace.instrument.http.SkipUriMatcherRegexImpl;
import org.springframework.boot.actuate.autoconfigure.ManagementServerProperties;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.sleuth.instrument.web.SleuthWebProperties;
import org.springframework.cloud.sleuth.instrument.web.TraceHttpAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration
 * Auto-configuration} that sets up common building blocks for both reactive
 * and servlet based web application.
 * compatible for spring boot 1.x
 *
 * @author Marcin Grzejszczak
 * @author hzfjd
 * @since 1.0.0
 * Change to support dynamic match condition.
 */
@Configuration
@ConditionalOnProperty(value = "spring.sleuth.web.enabled", matchIfMissing = true)
@ConditionalOnBean(Tracing.class)
@AutoConfigureAfter(TraceHttpAutoConfiguration.class)
@EnableConfigurationProperties(SleuthWebProperties.class)
public class TraceWebAutoConfigurationLegacy {



    @Bean
    @ConditionalOnProperty(name = "spring.sleuth.http.legacy.enabled", havingValue = "false", matchIfMissing = true)
    public HttpTracing sleuthHttpTracing(Tracing tracing) {
        return HttpTracing.newBuilder(tracing).serverParser(new HttpServerParserCustomed()).build();
    }



    @Configuration
    @ConditionalOnClass(ManagementServerProperties.class)
    @EnableConfigurationProperties(SleuthWebProperties.class)
    @ConditionalOnMissingBean(SkipUriMatcher.class)
    static class SkipPatternProviderConfig {

        @Bean
        @ConditionalOnBean(ManagementServerProperties.class)
        @RefreshScope
        public SkipUriMatcher skipPatternForManagementServerProperties(
                final ManagementServerProperties managementServerProperties,
                final SleuthWebProperties sleuthWebProperties) {
            return new SkipUriMatcherRegexImpl(getPatternForManagementServerProperties(
                    managementServerProperties,
                    sleuthWebProperties));
        }

        /**
         * Sets or appends {@link ManagementServerProperties#getContextPath()} to the skip
         * pattern. If neither is available then sets the default one
         */
        static Pattern getPatternForManagementServerProperties(
                ManagementServerProperties managementServerProperties,
                SleuthWebProperties sleuthWebProperties) {
            String skipPattern = sleuthWebProperties.getSkipPattern();
            String additionalSkipPattern = sleuthWebProperties.getAdditionalSkipPattern();
            String contextPath = managementServerProperties.getContextPath();

            if (StringUtils.hasText(skipPattern) && StringUtils.hasText(contextPath)) {
                return Pattern.compile(combinedPattern(skipPattern + "|" + contextPath + ".*", additionalSkipPattern));
            } else if (StringUtils.hasText(contextPath)) {
                return Pattern.compile(combinedPattern(contextPath + ".*", additionalSkipPattern));
            }
            return defaultSkipPattern(skipPattern, additionalSkipPattern);
        }

        @Bean
        @ConditionalOnMissingBean(ManagementServerProperties.class)
        public SkipUriMatcher defaultSkipPatternBeanIfManagementServerPropsArePresent(
                SleuthWebProperties sleuthWebProperties) {
            return new SkipUriMatcherRegexImpl(defaultSkipPattern(sleuthWebProperties.getSkipPattern(),
                                                                  sleuthWebProperties.getAdditionalSkipPattern()));
        }
    }

    @Bean
    @ConditionalOnMissingClass("org.springframework.boot.actuate.autoconfigure.ManagementServerProperties")
    @ConditionalOnMissingBean(
            SkipUriMatcher.class)
    public SkipUriMatcher defaultSkipPatternBean(SleuthWebProperties sleuthWebProperties) {
        return new SkipUriMatcherRegexImpl(defaultSkipPattern(sleuthWebProperties.getSkipPattern(),
                                                              sleuthWebProperties.getAdditionalSkipPattern()));
    }

    private static Pattern defaultSkipPattern(String skipPattern, String additionalSkipPattern) {
        return Pattern.compile(combinedPattern(skipPattern, additionalSkipPattern));
    }

    private static String combinedPattern(String skipPattern, String additionalSkipPattern) {
        String pattern = skipPattern;
        if (!StringUtils.hasText(skipPattern)) {
            pattern = SleuthWebProperties.DEFAULT_SKIP_PATTERN;
        }
        if (StringUtils.hasText(additionalSkipPattern)) {
            return pattern + "|" + additionalSkipPattern;
        }
        return pattern;
    }

}

