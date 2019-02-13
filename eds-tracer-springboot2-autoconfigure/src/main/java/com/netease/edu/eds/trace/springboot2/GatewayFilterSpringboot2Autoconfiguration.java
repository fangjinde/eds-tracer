package com.netease.edu.eds.trace.springboot2;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.cloud.gateway.config.GatewayAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.netease.edu.eds.trace.springboot2.instrument.http.TraceHttpHeadersFilter;

/**
 * @author hzfjd
 * @create 19/2/13
 **/

@Configuration
@ConditionalOnProperty(value = "spring.sleuth.web.enabled", matchIfMissing = true)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@AutoConfigureBefore(GatewayAutoConfiguration.class)
public class GatewayFilterSpringboot2Autoconfiguration {

	@Bean
	@ConditionalOnMissingBean
	public TraceHttpHeadersFilter traceHttpHeadersFilter() {
		return new TraceHttpHeadersFilter();
	}

}
