package com.netease.edu.eds.trace.springboot2;

import brave.Tracing;
import com.netease.edu.eds.trace.instrument.http.ServerSkipUriMatcher;
import com.netease.edu.eds.trace.support.ServerSkipUriMatcherRegexImpl;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementServerProperties;
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

import static com.netease.edu.eds.trace.springbootcompatible.utils.SkipPatternUtils.combinedPattern;
import static com.netease.edu.eds.trace.springbootcompatible.utils.SkipPatternUtils.defaultSkipPattern;

/**
 * @author hzfjd
 * @create 19/1/28
 **/
@Configuration
@ConditionalOnProperty(value = "spring.sleuth.web.enabled", matchIfMissing = true)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.ANY)
@ConditionalOnBean(Tracing.class)
@AutoConfigureAfter(TraceHttpAutoConfiguration.class)
@EnableConfigurationProperties(SleuthWebProperties.class)
@ConditionalOnClass(ManagementServerProperties.class)
public class TraceWebSpringboot2AutoConfiguration {

	@Configuration
	@ConditionalOnMissingBean(ServerSkipUriMatcher.class)
	public static class SkipPatternProviderConfig {

		@Bean
		@ConditionalOnBean(ManagementServerProperties.class)
		@RefreshScope
		public ServerSkipUriMatcher skipPatternForManagementServerProperties(
				final ManagementServerProperties managementServerProperties,
				final SleuthWebProperties sleuthWebProperties) {
			return new ServerSkipUriMatcherRegexImpl(
					getPatternForManagementServerProperties(managementServerProperties,
							sleuthWebProperties));
		}

		/**
		 * Sets or appends
		 * {@link ManagementServerProperties#getServlet()#getContextPath()} to the skip
		 * pattern. If neither is available then sets the default one
		 */
		static Pattern getPatternForManagementServerProperties(
				ManagementServerProperties managementServerProperties,
				SleuthWebProperties sleuthWebProperties) {
			String skipPattern = sleuthWebProperties.getSkipPattern();
			String additionalSkipPattern = sleuthWebProperties.getAdditionalSkipPattern();
			String contextPath = managementServerProperties.getServlet().getContextPath();

			if (StringUtils.hasText(skipPattern) && StringUtils.hasText(contextPath)) {
				return Pattern.compile(combinedPattern(
						skipPattern + "|" + contextPath + ".*", additionalSkipPattern));
			}
			else if (StringUtils.hasText(contextPath)) {
				return Pattern.compile(
						combinedPattern(contextPath + ".*", additionalSkipPattern));
			}
			return defaultSkipPattern(skipPattern, additionalSkipPattern);
		}

		@Bean
		@ConditionalOnMissingBean(ManagementServerProperties.class)
		public ServerSkipUriMatcher defaultSkipPatternBeanIfManagementServerPropsArePresent(
				SleuthWebProperties sleuthWebProperties) {

			return new ServerSkipUriMatcherRegexImpl(
					defaultSkipPattern(sleuthWebProperties.getSkipPattern(),
							sleuthWebProperties.getAdditionalSkipPattern()));

		}

	}

}
