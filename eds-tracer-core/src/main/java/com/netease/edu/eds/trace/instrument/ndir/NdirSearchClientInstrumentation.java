package com.netease.edu.eds.trace.instrument.ndir;/**
 * Created by hzfjd on 18/4/19.
 */

import brave.Span;
import brave.Tracer;
import com.netease.edu.eds.trace.constants.SpanType;
import com.netease.edu.eds.trace.spi.TraceAgentInstrumetation;
import com.netease.edu.eds.trace.support.DefaultAgentBuilderListener;
import com.netease.edu.eds.trace.support.SpringBeanFactorySupport;
import com.netease.edu.eds.trace.utils.ExceptionStringUtils;
import com.netease.edu.eds.trace.utils.SpanUtils;
import com.netease.ndir.common.ResponseCode;
import com.netease.ndir.common.exception.NDirException;
import com.netease.ndir.common.search.SearchResultView;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.matcher.ElementMatchers;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.springframework.beans.factory.BeanFactory;
import zipkin2.Endpoint;

import java.io.ByteArrayOutputStream;
import java.lang.instrument.Instrumentation;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * @author hzfjd
 * @create 18/4/19
 */
public class NdirSearchClientInstrumentation implements TraceAgentInstrumetation {

    @Override public void premain(Map<String, String> props, Instrumentation inst) {

        new AgentBuilder.Default().type(
                ElementMatchers.namedIgnoreCase("com.netease.ndir.client.NDirSearchClient2")).transform(
                (builder, typeDescription, classloader, javaModule) ->
                        builder.method(ElementMatchers.namedIgnoreCase("execute").and(
                                ElementMatchers.isDeclaredBy(typeDescription))).intercept(
                                MethodDelegation.to(TraceInterceptor.class))).with(
                DefaultAgentBuilderListener.getInstance()).installOn(
                inst);
    }

    public static class TraceInterceptor {

        //, @Origin String methodString,
        public static SearchResultView execute(@Argument(0) HttpRequestBase request,
                                               @SuperCall Callable<SearchResultView> callable) throws NDirException {

            NdirTracing ndirTracing = null;
            BeanFactory beanFactory = SpringBeanFactorySupport.getBeanFactory();
            if (beanFactory != null) {
                ndirTracing = beanFactory.getBean(NdirTracing.class);
            }

            if (ndirTracing == null) {
                try {
                    return callable.call();
                } catch (Exception e) {
                    if (e instanceof NDirException) {
                        throw (NDirException) e;
                    } else {
                        throw new NDirException(ResponseCode.UNKNOWN, e);
                    }
                }

            }

            Span span = ndirTracing.tracing().tracer().nextSpan();
            SpanUtils.safeTag(span, SpanType.TAG_KEY, SpanType.NDIR);
            if (!span.isNoop()) {
                URI uri = request.getURI();
                String spanName = uri.getPath() + uri.getQuery();
                if (spanName.length() > 50) {
                    spanName = spanName.substring(0, 50);
                }
                span.kind(Span.Kind.CLIENT).name(spanName);
                span.remoteEndpoint(Endpoint.newBuilder().ip(uri.getHost()).port(uri.getPort()).build());
                span.tag("search_uri", uri.toString());

                try {
                    if (request instanceof HttpPost) {
                        HttpPost postRequest = (HttpPost) request;
                        HttpEntity httpEntity = postRequest.getEntity();
                        if (httpEntity instanceof StringEntity) {
                            StringEntity stringEntity = (StringEntity) httpEntity;
                            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                            stringEntity.writeTo(byteArrayOutputStream);
                            String requestBodyStr = byteArrayOutputStream.toString("utf-8");
                            span.tag("search_body", requestBodyStr);
                        }
                    }
                } catch (Exception e) {
                    span.tag("search_body", ExceptionStringUtils.getStackTraceString(e));
                }
                span.start();
            }

            try (Tracer.SpanInScope spanInScope = ndirTracing.tracing().tracer().withSpanInScope(span)) {
                return callable.call();
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
