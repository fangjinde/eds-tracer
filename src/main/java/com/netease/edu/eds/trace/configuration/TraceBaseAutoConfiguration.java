package com.netease.edu.eds.trace.configuration;/**
                                                 * Created by hzfjd on 18/3/1.
                                                 */

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.sleuth.ErrorParser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.netease.edu.eds.trace.support.EduExceptionMessageErrorParser;
import com.netease.edu.eds.trace.support.SpringBeanFactorySupport;

/**
 * @author hzfjd
 * @create 18/3/1
 */
@Configuration
@EnableConfigurationProperties({ KafkaProperties.class })
@ConditionalOnProperty(value = "spring.zipkin.enabled", matchIfMissing = true)
@AutoConfigureBefore(EduZipkinAutoConfiguration.class)
public class TraceBaseAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ErrorParser eduExceptionMessageErrorParser() {
        return new EduExceptionMessageErrorParser();
    }

    @Bean
    @ConditionalOnMissingBean
    public SpringBeanFactorySupport springBeanFactorySupport() {
        return new SpringBeanFactorySupport();
    }
}
