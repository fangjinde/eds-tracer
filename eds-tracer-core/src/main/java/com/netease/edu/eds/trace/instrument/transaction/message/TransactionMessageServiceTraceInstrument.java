package com.netease.edu.eds.trace.instrument.transaction.message;

import brave.Span;
import brave.Tracer;
import brave.Tracing;
import brave.propagation.Propagation;
import brave.propagation.TraceContext;
import com.netease.edu.eds.trace.core.Invoker;
import com.netease.edu.eds.trace.support.AbstractTraceAgentInstrumetation;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bind.annotation.*;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.utility.JavaModule;

import java.lang.reflect.Method;
import java.util.Map;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * @author hzfjd
 * @create 18/10/22
 **/
public class TransactionMessageServiceTraceInstrument extends AbstractTraceAgentInstrumetation {

    @Override
    protected ElementMatcher.Junction defineTypeMatcher(Map<String, String> props) {
        return namedIgnoreCase("com.netease.edu.transaction.message.service.TransactionMessageService");
    }

    @Override
    protected ElementMatcher.Junction defineMethodMatcher(Map<String, String> props, TypeDescription typeDescription,
                                                          ClassLoader classLoader, JavaModule module) {
        // public Long prepare(String environment, String provider, String topic, Map<String, Object> header, String
        // body)

        ElementMatcher.Junction prepare5 = isDeclaredBy(typeDescription).and(namedIgnoreCase(TransactionMessageConstants.PREPARE_METHOD_NAME)).and(takesArguments(5)).and(isPublic());

        // public void recheck(Long id)
        ElementMatcher.Junction recheck1 = isDeclaredBy(typeDescription).and(namedIgnoreCase(TransactionMessageConstants.RECHECK_METHOD_NAME)).and(takesArguments(1).and(isPublic()));

        // public Boolean reconsume(Long id)
        ElementMatcher.Junction reconsume1 = isDeclaredBy(typeDescription).and(namedIgnoreCase(TransactionMessageConstants.RECONSUME_METHOD_NAME)).and(takesArguments(1)).and(isPublic());

        return prepare5.or(recheck1).or(reconsume1);

    }

    @Override
    protected Class defineInterceptorClass(Map<String, String> props) {
        return Interceptor.class;
    }

    public static class Interceptor {

        @RuntimeType
        public static Object intercept(@AllArguments Object[] args, @Morph Invoker invoker, @Origin Method method,
                                       @This Object proxy) {

            // do for "getMessageListener" method
            if (method.getName().equals(TransactionMessageConstants.PREPARE_METHOD_NAME)) {
                return prepare(args, invoker, method, proxy);
            } else if (method.getName().equals(TransactionMessageConstants.RECHECK_METHOD_NAME)
                       || method.getName().equals(TransactionMessageConstants.RECONSUME_METHOD_NAME)) {
                return recheckOrResume(args, invoker, method, proxy);
            } else {
                return invoker.invoke(args);
            }

        }

        private static Propagation.Setter<Map<String, Object>, String> SETTER = (Map<String, Object> carrier,
                                                                                 String key, String value) -> {
            carrier.put(key, value);

        };

        /**
         * prepare是远程dubbo或者http方法，所以在外层拦截器上已经做过trace包裹。
         * 
         * @param args
         * @param invoker
         * @param method
         * @param proxy
         * @return
         */
        private static Object prepare(Object[] args, Invoker invoker, Method method, Object proxy) {
            Tracing tracing = Tracing.current();
            Tracer tracer = null;
            if (tracing != null) {
                tracer = tracing.tracer();
            }

            Span span = null;
            if (tracer != null) {
                span = tracer.currentSpan();
            }

            // prepare是远程dubbo或者http方法，所以在外层拦截器上已经做过trace包裹。
            if (span == null || span.isNoop()) {
                return invoker.invoke(args);
            }

            Object args3 = args[3];
            if (!(args3 instanceof Map)) {
                return invoker.invoke(args);
            }

            Map<String, Object> header = (Map<String, Object>) args3;
            TraceContext.Injector<Map<String, Object>> injector = tracing.propagation().injector(SETTER);
            injector.inject(span.context(), header);
            return invoker.invoke(args);

        }

        private static Object recheckOrResume(Object[] args, Invoker invoker, Method method, Object proxy) {

            Tracing tracing = Tracing.current();
            Tracer tracer = null;
            if (tracing != null) {
                tracer = tracing.tracer();
            }
            if (tracer == null) {
                return invoker.invoke(args);
            }

            try {
                TransactionMessageRestoreMethodContext.setMethodName(method.getName());
                return invoker.invoke(args);
            } finally {
                TransactionMessageRestoreMethodContext.reset();
            }

        }

    }
}
