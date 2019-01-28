package com.netease.edu.eds.trace.springbootcompatible.configuration;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.sleuth.instrument.web.SleuthWebProperties;
import org.springframework.cloud.sleuth.instrument.web.TraceHttpAutoConfiguration;
import org.springframework.context.annotation.Bean;

import com.netease.edu.eds.trace.springbootcompatible.spi.SkipUriMatcher;
import com.netease.edu.eds.trace.springbootcompatible.support.SkipUriMatcherRegexImpl;
import com.netease.edu.eds.trace.springbootcompatible.utils.SkipPatternUtils;
import org.springframework.context.annotation.Configuration;

/**
 * @author hzfjd
 * @create 19/1/28
 **/
@Configuration
@ConditionalOnProperty(value = "spring.sleuth.web.enabled", matchIfMissing = true)
@AutoConfigureAfter(TraceHttpAutoConfiguration.class)
@EnableConfigurationProperties(SleuthWebProperties.class)
public class TraceWebSpringBootCompatibleAutoConfiguration {

    @Bean
    @ConditionalOnMissingClass({ "org.springframework.boot.actuate.autoconfigure.ManagementServerProperties",
                                 "org.springframework.boot.actuate.autoconfigure.web.server.ManagementServerProperties" })
    @ConditionalOnMissingBean(SkipUriMatcher.class)
    public SkipUriMatcher defaultSkipPatternBean(SleuthWebProperties sleuthWebProperties) {
        return new SkipUriMatcherRegexImpl(SkipPatternUtils.defaultSkipPattern(sleuthWebProperties.getSkipPattern(),
                                                                               sleuthWebProperties.getAdditionalSkipPattern()));
    }

}
