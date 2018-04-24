package com.netease.edu.eds.trace.instrument.ndir;/**
 * Created by hzfjd on 18/4/19.
 */

import brave.Span;
import brave.Tracer;
import com.netease.edu.eds.trace.spi.TraceAgentInstrumetation;
import com.netease.edu.eds.trace.support.SpringBeanFactorySupport;
import com.netease.edu.eds.trace.utils.ExceptionStringUtils;
import com.netease.edu.util.collection.BaseQuery;
import com.netease.edu.util.collection.PaginationResult;
import com.netease.ndir.client.config.SearchConfig;
import com.netease.ndir.client.config.SortField;
import com.netease.ndir.client.exception.NDirClientException;
import com.netease.ndir.common.exception.NDirException;
import com.netease.ndir.common.search.SearchResultView;
import com.netease.ndir.common.syntax.NDirQuery;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.matcher.ElementMatchers;
import org.apache.http.client.methods.HttpRequestBase;
import org.springframework.beans.factory.BeanFactory;
import zipkin2.Endpoint;

import java.io.IOException;
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
                        ElementMatchers.namedIgnoreCase(
                                "com.netease.edu.persist.search.service.AbstractNDirBaseSearchDao")).transform(
                        (builder, typeDescription, classloader, javaModule) ->
                                builder.method(ElementMatchers.namedIgnoreCase("getByQueryCondition")).intercept(
                                        MethodDelegation.to(Trace5.class))
                ).installOn(inst);


        //

        new AgentBuilder.Default().type(
                ElementMatchers.namedIgnoreCase("com.netease.ndir.client.config.SearchConfig")).transform(
                (builder, typeDescription, classloader, javaModule) ->
                        builder.method(ElementMatchers.namedIgnoreCase("setSortFields")).intercept(
                                MethodDelegation.to(Trace3.class))
        ).installOn(inst);


        new AgentBuilder.Default().type(
                ElementMatchers.namedIgnoreCase("com.netease.ndir.client.NDirSearchClient2")).transform(
                (builder, typeDescription, classloader, javaModule) ->
                        builder.method(ElementMatchers.namedIgnoreCase("globalSearch")).intercept(
                                MethodDelegation.to(Trace6.class))
        ).installOn(inst);

        //        new AgentBuilder.Default().type(
        //                ElementMatchers.namedIgnoreCase("com.netease.ndir.client.NDirSearchClient2")).transform(
        //                (builder, typeDescription, classloader, javaModule) ->
        //                        builder.method(ElementMatchers.namedIgnoreCase("execute")).intercept(
        //                                MethodDelegation.to(TraceInterceptor.class)).method(
        //                                ElementMatchers.namedIgnoreCase("globalSearch")).intercept(
        //                                MethodDelegation.to(Trace2.class))).installOn(inst);
    }

    //.and(ElementMatchers.takesArguments(1))

    public static class Trace6 {

        public static SearchResultView globalSearch(String index, NDirQuery query,
                                                    int offset, int length, SearchConfig config) {
            System.out.println("execute");
            return null;
        }
    }

    public static class Trace5 {

        public static PaginationResult getByQueryCondition(NDirQuery query, BaseQuery baseQuery, SearchConfig config) {
            System.out.println("execute");
            return null;
        }
    }

    public static class Trace3 {

        public static void setSortFields(SortField... sortFields) {
            System.out.println(sortFields);
        }
    }

    public static class Trace4 {

        public static SearchResultView execute(HttpRequestBase request)
                throws NDirException {
            System.out.println("execute");
            return null;
        }
    }

    public static class Trace2 {

        public static SearchResultView globalSearch(String index, NDirQuery query,
                                                    int offset, int length, SearchConfig config
        ) throws IOException,
                 NDirException,
                 NDirClientException {

            NdirTracing ndirTracing = null;
            BeanFactory beanFactory = SpringBeanFactorySupport.getBeanFactory();
            if (beanFactory != null) {
                ndirTracing = beanFactory.getBean(NdirTracing.class);
            }

            if (ndirTracing == null) {
                return null;

            }

            // URI uri = request.getURI();
            Span span = ndirTracing.tracing().tracer().nextSpan().kind(Span.Kind.CLIENT).name("ndir");
            // span.remoteEndpoint(Endpoint.newBuilder().ip(uri.getHost()).port(uri.getPort()).build());
            // span.tag("search_uri", uri.toString());

            try (Tracer.SpanInScope spanInScope = ndirTracing.tracing().tracer().withSpanInScope(span)) {
                return null;
            } catch (Exception e) {
                span.tag("ndir_error", ExceptionStringUtils.getStackTraceString(e));
                throw new NDirClientException("c", e);
            } finally {
                span.finish();
            }

        }
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
                    return null;
                }

            }

            URI uri = request.getURI();
            Span span = ndirTracing.tracing().tracer().nextSpan().kind(Span.Kind.CLIENT).name("ndir");
            span.remoteEndpoint(Endpoint.newBuilder().ip(uri.getHost()).port(uri.getPort()).build());
            span.tag("search_uri", uri.toString());

            try (Tracer.SpanInScope spanInScope = ndirTracing.tracing().tracer().withSpanInScope(span)) {
                return callable.call();
            } catch (Exception e) {
                span.tag("ndir_error", ExceptionStringUtils.getStackTraceString(e));
                return null;
            } finally {
                span.finish();
            }

        }
    }
}
