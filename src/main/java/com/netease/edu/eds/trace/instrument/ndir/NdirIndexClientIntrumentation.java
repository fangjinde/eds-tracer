package com.netease.edu.eds.trace.instrument.ndir;/**
 * Created by hzfjd on 18/4/25.
 */

import brave.Span;
import brave.Tracer;
import com.netease.edu.eds.trace.spi.TraceAgentInstrumetation;
import com.netease.edu.eds.trace.support.DefaultAgentBuilderListener;
import com.netease.edu.eds.trace.support.SpringBeanFactorySupport;
import com.netease.edu.eds.trace.utils.ExceptionStringUtils;
import com.netease.ndir.client.base.NDirHttpClientBase;
import com.netease.ndir.client.util.WrappedIndexRequest;
import com.netease.ndir.common.ResponseCode;
import com.netease.ndir.common.exception.NDirException;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.matcher.ElementMatchers;
import org.springframework.beans.factory.BeanFactory;
import zipkin2.Endpoint;

import java.lang.instrument.Instrumentation;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

/**
 * @author hzfjd
 * @create 18/4/25
 */
public class NdirIndexClientIntrumentation implements TraceAgentInstrumetation {

    @Override public void premain(Map<String, String> props, Instrumentation inst) {
        new AgentBuilder.Default().type(
                ElementMatchers.namedIgnoreCase("com.netease.ndir.client.NDirIndexClient")).transform(
                (builder, typeDescription, classloader, javaModule) ->
                        builder.method(ElementMatchers.namedIgnoreCase("postwrappedIndexRequest").and(
                                ElementMatchers.isDeclaredBy(typeDescription))).intercept(
                                MethodDelegation.to(TraceInterceptor.class))).with(
                DefaultAgentBuilderListener.getInstance()).installOn(
                inst);
    }

    public static class TraceInterceptor {

        public static void postwrappedIndexRequest(@Argument(0) WrappedIndexRequest wrappedIndexRequest,
                                                   @Argument(1) String url, @SuperCall Runnable runnable)
                throws NDirException {

            NdirTracing ndirTracing = null;
            BeanFactory beanFactory = SpringBeanFactorySupport.getBeanFactory();
            if (beanFactory != null) {
                ndirTracing = beanFactory.getBean(NdirTracing.class);
            }

            URI uri = null;
            try {
                uri = new URI(url);
            } catch (URISyntaxException e) {

            }

            if (ndirTracing == null || uri == null) {
                try {
                    runnable.run();
                } catch (Exception e) {
                    if (e instanceof NDirException) {
                        throw (NDirException) e;
                    } else {
                        throw new NDirException(ResponseCode.UNKNOWN, e);
                    }
                }

            }

            Span span = ndirTracing.tracing().tracer().nextSpan();
            if (!span.isNoop()) {
                String spanName = uri.getPath() + uri.getQuery();
                if (spanName.length() > 50) {
                    spanName = spanName.substring(0, 50);
                }
                span.kind(Span.Kind.CLIENT).name(spanName);
                span.remoteEndpoint(Endpoint.newBuilder().ip(uri.getHost()).port(uri.getPort()).build());
                span.tag("index_uri", uri.toString());

                try {
                    String requestBodyStr = NDirHttpClientBase.getJosonInstance().toJson(wrappedIndexRequest);
                    span.tag("index_content", requestBodyStr);
                } catch (Exception e) {
                    span.tag("index_content", ExceptionStringUtils.getStackTraceString(e));
                }
                span.start();
            }

            try (Tracer.SpanInScope spanInScope = ndirTracing.tracing().tracer().withSpanInScope(span)) {
                runnable.run();
            } catch (Exception e) {
                span.tag("ndir_error", ExceptionStringUtils.getStackTraceString(e));
                if (e instanceof NDirException) {
                    throw (NDirException) e;
                } else {
                    throw new NDirException(ResponseCode.UNKNOWN, e);
                }
            } finally {
                span.finish();
            }

        }
    }
}
