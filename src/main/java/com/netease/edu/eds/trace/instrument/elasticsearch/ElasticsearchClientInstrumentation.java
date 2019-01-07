package com.netease.edu.eds.trace.instrument.elasticsearch;

import java.lang.instrument.Instrumentation;
import java.util.Map;
import java.util.concurrent.Callable;

import org.elasticsearch.action.Action;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequest;
import org.springframework.beans.factory.BeanFactory;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.matcher.ElementMatchers;
import brave.Span;
import brave.Tracer;

import com.netease.edu.eds.trace.constants.SpanType;
import com.netease.edu.eds.trace.spi.TraceAgentInstrumetation;
import com.netease.edu.eds.trace.support.DefaultAgentBuilderListener;
import com.netease.edu.eds.trace.support.SpringBeanFactorySupport;
import com.netease.edu.eds.trace.utils.ExceptionStringUtils;
import com.netease.edu.eds.trace.utils.SpanUtils;

public class ElasticsearchClientInstrumentation implements TraceAgentInstrumetation {

    @Override
    public void premain(Map<String, String> props, Instrumentation inst) {

        new AgentBuilder.Default().type(
            ElementMatchers.hasSuperType(ElementMatchers.namedIgnoreCase("org.elasticsearch.client.ElasticsearchClient"))).transform(
            (builder, typeDescription, classloader, javaModule) -> builder.method(
                ElementMatchers.namedIgnoreCase("execute").and(ElementMatchers.isDeclaredBy(typeDescription))).intercept(
                MethodDelegation.to(TraceInterceptor.class))).with(DefaultAgentBuilderListener.getInstance()).installOn(
            inst);
    }

    public static class TraceInterceptor {

        public static void execute(@Argument(0) Action action, @Argument(1) ActionRequest request,
            @Argument(2) ActionListener listener, @SuperCall Callable<Object> callable) throws Exception {

            ElasticsearchTracing tracing = null;
            BeanFactory beanFactory = SpringBeanFactorySupport.getBeanFactory();
            if (beanFactory != null) {
                tracing = beanFactory.getBean(ElasticsearchTracing.class);
            }

            if (tracing == null) {
                callable.call();
            }

            Span span = tracing.tracing().tracer().nextSpan();
            SpanUtils.safeTag(span, SpanType.TAG_KEY, SpanType.ELASTICSEARCH);
            if (!span.isNoop()) {
                try {
                    span.tag("search_body", request.toString());
                } catch (Exception e) {
                    span.tag("search_body", ExceptionStringUtils.getStackTraceString(e));
                }
                span.start();
            }

            try (Tracer.SpanInScope spanInScope = tracing.tracing().tracer().withSpanInScope(span)) {
                callable.call();
            } catch (Exception e) {
                span.tag("es_error", ExceptionStringUtils.getStackTraceString(e));

                throw e;
            } finally {
                span.finish();
            }

        }
    }

}
