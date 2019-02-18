package com.netease.edu.eds.trace.springbootcompatible.configuration;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.sleuth.instrument.web.SleuthWebProperties;
import org.springframework.cloud.sleuth.instrument.web.TraceHttpAutoConfiguration;
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

}
