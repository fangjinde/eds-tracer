package com.netease.edu.eds.trace.instrument.jobservice;

import brave.Span;
import brave.Tracer;
import brave.Tracing;
import com.netease.edu.eds.trace.core.Invoker;
import com.netease.edu.eds.trace.support.AbstractTraceAgentInstrumetation;
import com.netease.edu.eds.trace.utils.PropagationUtils;
import com.netease.edu.eds.trace.utils.SpanUtils;
import com.netease.edu.eds.trace.utils.TraceJsonUtils;
import com.netease.edu.job.dao.domain.DelayTask;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bind.annotation.*;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.utility.JavaModule;

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
        return sendDelayTaskMessage1;
    }

    @Override
    protected Class defineInterceptorClass(Map<String, String> props) {
        return Interceptor.class;
    }

    public static class Interceptor {

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

            Object arg0 = args[0];
            if (!(arg0 instanceof DelayTask)) {
                return invoker.invoke(args);
            }

            // 从DelayTask中恢复环境信息
            DelayTask delayTask = (DelayTask) arg0;
            PropagationUtils.setOriginEnvIfNotExists(span.context(), delayTask.getEnvironment());
            SpanUtils.safeTag(span, "DelayTask", TraceJsonUtils.toJson(delayTask));
            return invoker.invoke(args);

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
                span.name(method.getName()).kind(Span.Kind.PRODUCER).start();
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
