package com.netease.edu.eds.trace.instrument.http;

import brave.Span;
import brave.SpanCustomizer;
import brave.http.HttpTracing;
import com.netease.edu.eds.trace.spi.TraceAgentInstrumetation;
import com.netease.edu.eds.trace.support.DefaultAgentBuilderListener;
import com.netease.edu.eds.trace.support.SpringBeanFactorySupport;
import com.netease.edu.eds.trace.utils.ExceptionHandler;
import com.netease.edu.eds.trace.utils.SpanUtils;
import com.netease.edu.eds.trace.utils.TraceJsonUtils;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.*;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import org.directwebremoting.WebContext;
import org.directwebremoting.WebContextFactory;

import javax.servlet.http.HttpServletRequest;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * @author hzfjd
 * @create 18/10/8
 **/
public class DwrTraceInstrument implements TraceAgentInstrumetation {

    @Override
    public void premain(Map<String, String> props, Instrumentation inst) {

        ElementMatcher.Junction classMatcher = ElementMatchers.isAnnotatedWith(ElementMatchers.namedIgnoreCase("org.directwebremoting.annotations.RemoteProxy"));

        ElementMatcher.Junction methodMatcher = ElementMatchers.isPublic().and(ElementMatchers.isAnnotatedWith(ElementMatchers.namedIgnoreCase("org.directwebremoting.annotations.RemoteMethod")));

        addTransformer(classMatcher, methodMatcher, inst);
    }

    public static void addTransformer(ElementMatcher.Junction classMatcher, ElementMatcher.Junction methodMatcher,
                                      Instrumentation inst) {
        new AgentBuilder.Default().type(classMatcher)

                                  .transform((builder, typeDescription, classloader,
                                              javaModule) -> builder.method(methodMatcher

                                  ).intercept(MethodDelegation.to(TraceInterceptor.class))).with(DefaultAgentBuilderListener.getInstance()).installOn(inst);
    }

    public static class TraceInterceptor {

        @RuntimeType
        public static Object around(@AllArguments Object[] args, @SuperCall Callable<Object> callable,
                                    @Origin Method method, @This Object proxy) {

            HttpTracing httpTracing = SpringBeanFactorySupport.getBean(HttpTracing.class);
            if (httpTracing == null) {
                try {
                    return callable.call();
                } catch (Exception e) {
                    throw ExceptionHandler.wrapToRuntimeException(e);
                }
            }

            WebContext webContext = WebContextFactory.get();
            HttpServletRequest request = null;
            if (webContext != null) {
                request = webContext.getHttpServletRequest();
            }

            Span span = null;
            if (request != null) {
                span = (Span) request.getAttribute(SpanCustomizer.class.getName());

            }

            if (span == null) {
                try {
                    return callable.call();
                } catch (Exception e) {
                    throw ExceptionHandler.wrapToRuntimeException(e);
                }
            }

            HttpTagUtils.tagRequestHeaders(request, span);
            String argsJson = TraceJsonUtils.toJson(args);
            SpanUtils.safeTag(span, "args", argsJson);

            try {
                Object ret = callable.call();
                SpanUtils.safeTag(span, "return", TraceJsonUtils.toJson(ret));
                return ret;
            } catch (Exception e) {
                SpanUtils.tagErrorMark(span);
                SpanUtils.tagError(span, e);
                throw ExceptionHandler.wrapToRuntimeException(e);
            }

        }
    }
}
