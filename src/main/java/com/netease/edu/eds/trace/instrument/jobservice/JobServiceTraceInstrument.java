package com.netease.edu.eds.trace.instrument.jobservice;

import brave.Span;
import brave.Tracer;
import brave.Tracing;
import com.netease.edu.eds.trace.constants.CommonTagKeys;
import com.netease.edu.eds.trace.core.Invoker;
import com.netease.edu.eds.trace.support.AbstractTraceAgentInstrumetation;
import com.netease.edu.eds.trace.utils.*;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bind.annotation.*;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.utility.JavaModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * @author hzfjd
 * @create 18/10/24
 **/
public class JobServiceTraceInstrument extends AbstractTraceAgentInstrumetation {

    private static final String SEND_DELAY_TASK_MESSAGE_METHOD = "sendDelayTaskMessage";

    private static final String BUILD_QUEUE_NAME_METHOD        = "buildQueueName";

    private static final Logger logger                         = LoggerFactory.getLogger(JobServiceTraceInstrument.class);

    @Override
    protected ElementMatcher.Junction defineTypeMatcher(Map<String, String> props) {
        return namedIgnoreCase("com.netease.edu.job.service.impl.JobServiceImpl");
    }

    @Override
    protected ElementMatcher.Junction defineMethodMatcher(Map<String, String> props, TypeDescription typeDescription,
                                                          ClassLoader classLoader, JavaModule module) {

        // public void sendDelayTaskMessage(Long id)

        ElementMatcher.Junction sendDelayTaskMessage1 = isDeclaredBy(typeDescription).and(isPublic()).and(namedIgnoreCase(SEND_DELAY_TASK_MESSAGE_METHOD)).and(takesArguments(1));

        // private String buildQueueName(DelayTask delayTask)
        ElementMatcher.Junction buildQueueName1 = isDeclaredBy(typeDescription).and(isPrivate()).and(namedIgnoreCase(BUILD_QUEUE_NAME_METHOD)).and(takesArguments(1));
        return sendDelayTaskMessage1.or(buildQueueName1);
    }

    @Override
    protected Class defineInterceptorClass(Map<String, String> props) {
        return Interceptor.class;
    }

    public static class Interceptor {

        private static Field            s_delayTaskClazzEnvField = null;
        private static volatile boolean loadedEnvField           = false;

        @RuntimeType
        public static Object intercept(@AllArguments Object[] args, @Morph Invoker invoker, @Origin Method method,
                                       @This Object proxy) {

            // do for "send" method
            if (method.getName().equals(SEND_DELAY_TASK_MESSAGE_METHOD)) {
                return sendDelayTaskMessage(args, invoker, method, proxy);
            } else if (method.getName().equals(BUILD_QUEUE_NAME_METHOD)) {
                return buildQueueName(args, invoker, method, proxy);
            }

            else {
                return invoker.invoke(args);
            }

        }

        private static Object buildQueueName(Object[] args, Invoker invoker, Method method, Object proxy) {

            if (!JobServiceDeplaySendTraceContext.inDelaySend()) {
                return invoker.invoke(args);
            }

            Tracing tracing = Tracing.current();
            Tracer tracer = null;
            if (tracing != null) {
                tracer = tracing.tracer();
            }

            Span span = null;
            if (tracer != null) {
                span = tracer.currentSpan();
            }

            if (span == null) {
                return invoker.invoke(args);
            }

            // 从DelayTask中恢复环境信息
            Object delayTask = args[0];
            SpanUtils.safeTag(span, "DelayTask", TraceJsonUtils.toJson(delayTask));
            String originEnv = getEnvFromDelayTaskObject(delayTask, span);
            SpanUtils.tagPropagationInfos(span);
            PropagationUtils.setOriginEnvIfNotExists(span.context(), originEnv);

            return invoker.invoke(args);

        }

        private static String getEnvFromDelayTaskObject(Object delayTask, Span span) {

            Field envField = getDelayTaskEnvFieldWithCache();

            if (envField == null) {
                SpanUtils.safeTag(span, "GetEnvFromDelayTaskError", "no field of environment");
                logger.error("GetEnvFromDelayTaskError: no field of environment.");
                return null;
            }

            try {
                return (String) envField.get(delayTask);
            } catch (Exception e) {
                SpanUtils.safeTag(span, "GetEnvFromDelayTaskError", ExceptionStringUtils.getStackTraceString(e));
                logger.error("GetEnvFromDelayTaskError", e);
                return null;
            }

        }

        private static Field getDelayTaskEnvFieldWithCache() {

            if (loadedEnvField) {
                return s_delayTaskClazzEnvField;
            }

            synchronized (Interceptor.class) {

                if (loadedEnvField) {
                    return s_delayTaskClazzEnvField;
                }

                s_delayTaskClazzEnvField = getDelayTaskEnvironmentField();
                loadedEnvField = true;

            }

            return s_delayTaskClazzEnvField;

        }

        private static Field getDelayTaskEnvironmentField() {
            Class delayTaskClazz = null;
            try {
                delayTaskClazz = Class.forName("com.netease.edu.job.dao.domain.DelayTask");
            } catch (ClassNotFoundException e) {
                return null;
            }

            if (delayTaskClazz != null) {
                Field envField = ReflectionUtils.findField(delayTaskClazz, "environment", String.class);
                return envField;
            }

            return null;

        }

        private static Object sendDelayTaskMessage(Object[] args, Invoker invoker, Method method, Object proxy) {

            Tracing tracing = Tracing.current();
            Tracer tracer = null;
            if (tracing != null) {
                tracer = tracing.tracer();
            }
            if (tracer == null) {
                return invoker.invoke(args);
            }

            Span span = tracer.nextSpan();
            if (!span.isNoop()) {

                String className = method.getDeclaringClass().getSimpleName();
                String methodName = method.getName();
                span.name(className + "." + methodName).kind(Span.Kind.PRODUCER).start();
                SpanUtils.safeTag(span, CommonTagKeys.CLIENT_ENV, EnvironmentUtils.getCurrentEnv());
                SpanUtils.safeTag(span, CommonTagKeys.CLASS, className);
                SpanUtils.safeTag(span, CommonTagKeys.METHOD, methodName);
            }

            try (Tracer.SpanInScope spanInScope = tracer.withSpanInScope(span)) {
                JobServiceDeplaySendTraceContext.mark();
                SpanUtils.safeTagArgs(span, args);
                Object returnObj = invoker.invoke(args);
                SpanUtils.safeTagReturn(span, returnObj);
                return returnObj;

            } catch (Throwable e) {
                SpanUtils.safeTagError(span, e);
                throw e;
            } finally {
                JobServiceDeplaySendTraceContext.reset();
                span.finish();
            }

        }

    }
}
