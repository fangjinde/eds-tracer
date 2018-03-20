package com.netease.edu.eds.trace.configuration;/**
 * Created by hzfjd on 18/3/1.
 */

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author hzfjd
 * @create 18/3/1
 */
@Configuration
@EnableConfigurationProperties({KafkaProperties.class })
@ConditionalOnProperty(value = "spring.zipkin.enabled", matchIfMissing = true)
@AutoConfigureBefore(EduZipkinAutoConfiguration.class)
public class TraceBaseAutoConfiguration {

}
