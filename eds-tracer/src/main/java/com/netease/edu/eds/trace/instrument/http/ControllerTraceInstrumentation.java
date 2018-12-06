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
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.annotation.Annotation;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * @author hzfjd
 * @create 18/6/8
 **/
public class ControllerTraceInstrumentation implements TraceAgentInstrumetation {

    @Override
    public void premain(Map<String, String> props, Instrumentation inst) {
        ElementMatcher.Junction controllerAnnoMatch = ElementMatchers.isAnnotatedWith(Controller.class).or(ElementMatchers.isAnnotatedWith(RestController.class));
        // ElementMatcher.Junction inheritsConrollerAnnoMatch =
        // ElementMatchers.inheritsAnnotation(Controller.class).or(ElementMatchers.inheritsAnnotation(RestController.class));
        ElementMatcher.Junction typeMatch = ElementMatchers.not(ElementMatchers.isInterface()).and(controllerAnnoMatch);

        new AgentBuilder.Default().type(typeMatch)

                                  .transform((builder, typeDescription, classloader, javaModule) -> builder.method(

                                                                                                                   ElementMatchers.isPublic().and(ElementMatchers.isAnnotatedWith(RequestMapping.class))

                                  ).intercept(MethodDelegation.to(TraceInterceptor.class))).with(DefaultAgentBuilderListener.getInstance()).installOn(inst);

    }

    private static int detectRequestBodyParamIndex(Method method) {
        int indexOfRequestBodyParam = 0;
        Annotation[][] parametersAnnotations = method.getParameterAnnotations();
        for (Annotation[] paramAnnos : parametersAnnotations) {
            for (Annotation anno : paramAnnos) {
                if (anno instanceof RequestBody) {
                    return indexOfRequestBodyParam;
                }
            }
            indexOfRequestBodyParam++;
        }

        return -1;
    }

    public static class TraceInterceptor {

        @RuntimeType
        public static Object around(@AllArguments Object[] args, @SuperCall Callable<Object> callable,
                                    @Origin Method method) {

            HttpTracing httpTracing = SpringBeanFactorySupport.getBean(HttpTracing.class);
            if (httpTracing == null) {
                try {
                    return callable.call();
                } catch (Exception e) {
                    throw ExceptionHandler.wrapToRuntimeException(e);
                }
            }

            ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            HttpServletRequest request = null;
            Span span = null;
            if (servletRequestAttributes != null) {
                request = servletRequestAttributes.getRequest();
                if (request != null) {
                    span = (Span) request.getAttribute(SpanCustomizer.class.getName());
                    HttpTagUtils.tagRequestParams(request, span);
                    HttpTagUtils.tagRequestHeaders(request, span);
                }

            }

            if (span != null && !span.isNoop()) {
                int requestBodyParamIndex = detectRequestBodyParamIndex(method);
                if (requestBodyParamIndex >= 0 && requestBodyParamIndex < args.length) {
                    span.tag("requestBody", TraceJsonUtils.toJson(args[requestBodyParamIndex]));
                }
            }

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
