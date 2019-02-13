package com.netease.edu.eds.trace.instrument.jobservice;

import brave.Span;
import brave.Tracer;
import brave.Tracing;
import com.netease.edu.eds.trace.core.Invoker;
import com.netease.edu.eds.trace.support.AbstractTraceAgentInstrumetation;
import com.netease.edu.eds.trace.utils.PropagationUtils;
import com.netease.edu.eds.trace.utils.SpanUtils;
import com.netease.edu.eds.trace.utils.TraceJsonUtils;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bind.annotation.*;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.utility.JavaModule;
import org.apache.commons.lang.StringUtils;

import java.lang.reflect.Method;
import java.util.Map;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * @Deprecated 无法根据拼接好后的routingKey来截取环境。因为分割字符在queueName或者env中也会被包含
 * @author hzfjd
 * @create 18/10/24
 **/
@Deprecated
public class JobServiceMQSenderTraceInstrument extends AbstractTraceAgentInstrumetation {

    private static final String SEND_MESSAGE_METHOD = "sendMessage";

    @Override
    protected ElementMatcher.Junction defineTypeMatcher(Map<String, String> props) {
        return namedIgnoreCase("com.netease.edu.job.mq.MQSender");
    }

    @Override
    protected ElementMatcher.Junction defineMethodMatcher(Map<String, String> props, TypeDescription typeDescription,
                                                          ClassLoader classLoader, JavaModule module) {
        // public void sendMessage(String queue, Long messageId, String body)
        ElementMatcher.Junction sendMessage3 = namedIgnoreCase(SEND_MESSAGE_METHOD).and(takesArguments(3)).and(isDeclaredBy(typeDescription)).and(isPublic());
        return sendMessage3;
    }

    @Override
    protected Class defineInterceptorClass(Map<String, String> props) {
        return Interceptor.class;
    }

    public static class Interceptor {

        /**
         * see com.netease.edu.job.service.impl.JobServiceImpl
         */
        private static String JOB_ROUTING_KEY_ENV_ID_CONNECTOR = "-";

        @RuntimeType
        public static Object intercept(@AllArguments Object[] args, @Morph Invoker invoker, @Origin Method method,
                                       @This Object proxy) {

            // do for "send" method
            if (method.getName().equals(SEND_MESSAGE_METHOD)) {
                return sendMessage(args, invoker, method, proxy);
            } else {
                return invoker.invoke(args);
            }

        }

        private static Object sendMessage(Object[] args, Invoker invoker, Method method, Object proxy) {
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
            if (!(arg0 instanceof String)) {
                return invoker.invoke(args);
            }

            // 从DelayTask中恢复环境信息
            String routingKey = (String) arg0;
            String originEnv = getEnvFromRoutingKey(routingKey);

            PropagationUtils.setOriginEnvIfNotExists(span.context(), originEnv);
            SpanUtils.safeTag(span, "jobMessage", TraceJsonUtils.toJson(args));
            return invoker.invoke(args);
        }

        /**
         * @Deprecated 无法根据拼接好后的routingKey来截取环境。因为分割字符在queueName或者env中也会被包含
         * @param routingKey
         * @return
         */
        private static String getEnvFromRoutingKey(String routingKey) {

            if (StringUtils.isBlank(routingKey)) {
                return null;
            }

            String[] fragments = routingKey.split(JOB_ROUTING_KEY_ENV_ID_CONNECTOR);

            if (fragments == null || fragments.length < 2) {
                return null;
            }

            return fragments[fragments.length - 1];

        }

    }
}
