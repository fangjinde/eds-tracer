package com.netease.edu.eds.shuffle.dubbo;

import brave.Span;
import brave.Tracer;
import brave.Tracing;
import com.alibaba.dubbo.rpc.RpcException;
import com.netease.edu.eds.shuffle.core.EnvironmentShuffleUtils;
import com.netease.edu.eds.shuffle.core.ShuffleSwitch;
import com.netease.edu.eds.trace.core.Invoker;
import com.netease.edu.eds.trace.support.AbstractTraceAgentInstrumetation;
import com.netease.edu.eds.trace.utils.TraceJsonUtils;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bind.annotation.*;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.utility.JavaModule;

import java.lang.reflect.Method;
import java.util.Map;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * @author hzfjd
 * @create 19/1/21
 **/
public class AbstractClusterInvokerShuffleInstrument extends AbstractTraceAgentInstrumetation {

    @Override
    protected ElementMatcher.Junction defineTypeMatcher(Map<String, String> props) {
        return namedIgnoreCase("com.alibaba.dubbo.rpc.cluster.support.AbstractClusterInvoker");
    }

    @Override
    protected ElementMatcher.Junction defineMethodMatcher(Map<String, String> props, TypeDescription typeDescription,
                                                          ClassLoader classLoader, JavaModule module) {
        // protected void checkInvokers(List<Invoker<T>> invokers, Invocation invocation)
        ElementMatcher.Junction checkInvokers2 = namedIgnoreCase("checkInvokers").and(isProtected()).and(isDeclaredBy(typeDescription)).and(takesArguments(2));

        return checkInvokers2;
    }

    @Override
    protected Class defineInterceptorClass(Map<String, String> props) {
        return Interceptor.class;
    }

    public static class Interceptor {

        @RuntimeType
        public static Object intercept(@AllArguments Object[] args, @Morph Invoker invoker, @Origin Method method,
                                       @This Object proxy) {

            if (!ShuffleSwitch.isTurnOn()) {
                return invoker.invoke(args);
            }

            Tracer tracer = Tracing.currentTracer();
            Span span = null;
            if (tracer != null) {
                span = tracer.currentSpan();
            }

            // 没有追踪中的请求，肯定也无法做穿梭。
            if (span == null) {
                return invoker.invoke(args);
            }

            try {
                return invoker.invoke(args);
            } catch (RpcException e) {
                // 对于no provider异常，定制异常文案，增加实际请求环境信息。
                if (e.getMessage() != null && e.getMessage().contains("No provider available for the service")) {
                    String errorMessage = e.getMessage() + " After shuffled, actually accessing provider environments are: "
                                          + TraceJsonUtils.toJson(EnvironmentShuffleUtils.getEnvironmentsForPropagationSelection());
                    throw new RpcException(errorMessage, e);
                } else {
                    throw e;
                }

            }

        }

    }
}
