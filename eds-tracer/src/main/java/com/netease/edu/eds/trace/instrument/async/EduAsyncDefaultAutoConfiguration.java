package com.netease.edu.eds.trace.instrument.async;

import brave.Tracer;
import brave.Tracing;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.sleuth.SpanNamer;
import org.springframework.cloud.sleuth.TraceKeys;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.AsyncConfigurerSupport;

import java.util.concurrent.Executor;

/**
 * @author hzfjd
 * @create 18/7/9
 **/
@Configuration
@ConditionalOnProperty(value = "spring.sleuth.async.enabled", matchIfMissing = true)
@ConditionalOnBean(Tracing.class)
public class EduAsyncDefaultAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(AsyncConfigurer.class)
    @ConditionalOnProperty(value = "spring.sleuth.async.configurer.enabled", matchIfMissing = true)
    public AsyncConfigurer defaultTracedAsyncConfigurer(BeanFactory beanFactory) {
        return new DefaultAsyncConfigurerSupport(beanFactory);
    }

    static class DefaultAsyncConfigurerSupport extends AsyncConfigurerSupport {

        DefaultAsyncConfigurerSupport(BeanFactory beanFactory) {
            this.beanFactory = beanFactory;
        }

        private BeanFactory beanFactory;

        @Override
        public Executor getAsyncExecutor() {
            return new EduLazyTraceExecutor(this.beanFactory, new SimpleAsyncTaskExecutor());
        }
    }

    @Bean
    public EduTraceAsyncAspect eduTraceAsyncAspect(Tracer tracer, SpanNamer spanNamer, TraceKeys traceKeys) {
        return new EduTraceAsyncAspect(tracer, spanNamer, traceKeys);
    }

    @Bean
    public EduExecutorBeanPostProcessor executorBeanPostProcessor(BeanFactory beanFactory) {
        return new EduExecutorBeanPostProcessor(beanFactory);
    }

}
