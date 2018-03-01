package com.netease.edu.boot.trace.configuration;/**



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

import brave.sampler.Sampler;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.sleuth.autoconfig.TraceAutoConfiguration;
import org.springframework.cloud.sleuth.sampler.ProbabilityBasedSampler;
import org.springframework.cloud.sleuth.sampler.SamplerProperties;
import org.springframework.cloud.sleuth.zipkin2.DefaultZipkinRestTemplateCustomizer;
import org.springframework.cloud.sleuth.zipkin2.ZipkinProperties;
import org.springframework.cloud.sleuth.zipkin2.ZipkinRestTemplateCustomizer;
import org.springframework.cloud.sleuth.zipkin2.sender.ZipkinSenderConfigurationImportSelector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import zipkin2.Span;
import zipkin2.reporter.*;

import java.util.concurrent.TimeUnit;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration Auto-configuration}
 * enables reporting to Zipkin via HTTP. Has a default {@link brave.sampler.Sampler} set as
 * {@link org.springframework.cloud.sleuth.sampler.ProbabilityBasedSampler}.
 * The {@link org.springframework.cloud.sleuth.zipkin2.ZipkinRestTemplateCustomizer} allows you to customize the {@link org.springframework.web.client.RestTemplate}
 * that is used to send Spans to Zipkin. Its default implementation - {@link org.springframework.cloud.sleuth.zipkin2.DefaultZipkinRestTemplateCustomizer}
 * adds the GZip compression.
 *
 * @author Spencer Gibb
 * @see org.springframework.cloud.sleuth.sampler.ProbabilityBasedSampler
 * @see org.springframework.cloud.sleuth.zipkin2.ZipkinRestTemplateCustomizer
 * @see org.springframework.cloud.sleuth.zipkin2.DefaultZipkinRestTemplateCustomizer
 * @since 1.0.0
 */
@Configuration
@EnableConfigurationProperties({ ZipkinProperties.class, SamplerProperties.class })
@ConditionalOnProperty(value = "spring.zipkin.enabled", matchIfMissing = true)
@AutoConfigureBefore(TraceAutoConfiguration.class)
@Import(ZipkinSenderConfigurationImportSelector.class)
public class EduZipkinAutoConfiguration {




    /**
     * Accepts a sender so you can plug-in any standard one. Returns a Reporter so you can also
     * replace with a standard one.
     */
    @Bean
    @ConditionalOnMissingBean
    public Reporter<Span> reporter(
            ReporterMetrics reporterMetrics,
            ZipkinProperties zipkin,
            Sender sender
    ) {
        return AsyncReporter.builder(sender)
                .queuedMaxSpans(1000) // historical constraint. Note: AsyncReporter supports memory bounds
                .messageTimeout(zipkin.getMessageTimeout(), TimeUnit.SECONDS)
                .metrics(reporterMetrics)
                .build(zipkin.getEncoder());
    }

    @Bean
    @ConditionalOnMissingBean
    public ZipkinRestTemplateCustomizer zipkinRestTemplateCustomizer(ZipkinProperties zipkinProperties) {
        return new DefaultZipkinRestTemplateCustomizer(zipkinProperties);
    }

    @Bean
    @ConditionalOnMissingBean ReporterMetrics sleuthReporterMetrics() {
        return new InMemoryReporterMetrics();
    }

    @Configuration
    @ConditionalOnClass(RefreshScope.class)
    protected static class RefreshScopedProbabilityBasedSamplerConfiguration {

        @Bean
        @RefreshScope
        @ConditionalOnMissingBean
        public Sampler defaultTraceSampler(SamplerProperties config) {
            return new ProbabilityBasedSampler(config);
        }
    }

    @Configuration
    @ConditionalOnMissingClass("org.springframework.cloud.context.config.annotation.RefreshScope")
    protected static class NonRefreshScopeProbabilityBasedSamplerConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public Sampler defaultTraceSampler(SamplerProperties config) {
            return new ProbabilityBasedSampler(config);
        }
    }

}
