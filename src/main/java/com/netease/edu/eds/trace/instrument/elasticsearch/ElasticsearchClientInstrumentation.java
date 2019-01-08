package com.netease.edu.eds.trace.instrument.elasticsearch;

import java.lang.instrument.Instrumentation;
import java.util.Map;
import java.util.concurrent.Callable;

import org.elasticsearch.action.Action;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.client.ElasticsearchClient;
import org.springframework.beans.factory.BeanFactory;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.matcher.ElementMatcher;
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

        new AgentBuilder.Default().type(ElementMatchers.hasSuperType(ElementMatchers.is(ElasticsearchClient.class))).transform(
            (builder, typeDescription, classloader, javaModule) -> builder.method(
                ElementMatchers.namedIgnoreCase("execute").and(ElementMatchers.isDeclaredBy(typeDescription)).and(
                    ElementMatchers.returns(TypeDescription.VOID))).intercept(
                MethodDelegation.to(TraceVoidReturnInterceptor.class))).with(DefaultAgentBuilderListener.getInstance()).installOn(
            inst);

        new AgentBuilder.Default().type(ElementMatchers.hasSuperType(ElementMatchers.is(ElasticsearchClient.class))).transform(
            (builder, typeDescription, classloader, javaModule) -> builder.method(
                ElementMatchers.namedIgnoreCase("execute").and(ElementMatchers.isDeclaredBy(typeDescription)).and(
                    ElementMatchers.returns(new TypeDescription.ForLoadedType(ActionFuture.class)))).intercept(
                MethodDelegation.to(TraceActionFutureReturnInterceptor.class))).with(
            DefaultAgentBuilderListener.getInstance()).installOn(inst);

    }

    public static class TraceVoidReturnInterceptor {

        public static void execute(@Argument(0) Action action, @Argument(1) ActionRequest request,
            @Argument(2) ActionListener listener, @SuperCall Callable<Void> callable) {

            interceptElasticsearch(request, callable);
        }
    }

    public static class TraceActionFutureReturnInterceptor {

        public static ActionFuture execute(@Argument(0) Action action, @Argument(1) ActionRequest request,
            @SuperCall Callable<ActionFuture> callable) {

            return interceptElasticsearch(request, callable);
        }
    }

    private static <T> T interceptElasticsearch(ActionRequest request, Callable<T> callable) {
        ElasticsearchTracing tracing = null;
        BeanFactory beanFactory = SpringBeanFactorySupport.getBeanFactory();
        if (beanFactory != null) {
            tracing = beanFactory.getBean(ElasticsearchTracing.class);
        }

        if (tracing == null) {
            try {
                return callable.call();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
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
            return callable.call();
        } catch (RuntimeException e) {
            span.tag("es_error", ExceptionStringUtils.getStackTraceString(e));
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            span.finish();
        }
    }

}
