package com.netease.edu.eds.trace.instrument.dubbo;

import brave.Span;
import brave.Tracer;
import brave.Tracing;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.cluster.support.AbstractClusterInvoker;
import com.alibaba.dubbo.rpc.support.RpcUtils;
import com.netease.edu.eds.trace.core.Invoker;
import com.netease.edu.eds.trace.support.AbstractTraceAgentInstrumetation;
import com.netease.edu.eds.trace.utils.EnvironmentUtils;
import com.netease.edu.eds.trace.utils.PropagationUtils;
import com.netease.edu.eds.trace.utils.SpanUtils;
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
 * @create 19/1/18
 **/
public class AbstractClusterInvokerTraceInstrument extends AbstractTraceAgentInstrumetation {

    @Override
    protected ElementMatcher.Junction defineTypeMatcher(Map<String, String> props) {
        return namedIgnoreCase("com.alibaba.dubbo.rpc.cluster.support.AbstractClusterInvoker");
    }

    @Override
    protected ElementMatcher.Junction defineMethodMatcher(Map<String, String> props, TypeDescription typeDescription,
                                                          ClassLoader classLoader, JavaModule module) {
        // com.alibaba.dubbo.rpc.cluster.support.AbstractClusterInvoker.invoke
        // public Result invoke(final Invocation invocation) throws RpcException
        ElementMatcher.Junction invoke1 = namedIgnoreCase("invoke").and(isPublic()).and(isDeclaredBy(typeDescription)).and(takesArguments(1));

        return invoke1;
    }

    @Override
    protected Class defineInterceptorClass(Map<String, String> props) {
        return Interceptor.class;
    }

    public static class Interceptor {

        @RuntimeType
        public static Object intercept(@AllArguments Object[] args, @Morph Invoker invoker, @Origin Method method,
                                       @This Object proxy) {

            Tracer tracer = Tracing.currentTracer();
            if (tracer == null) {
                return invoker.invoke(args);
            }

            Invocation invocation = (Invocation) args[0];
            AbstractClusterInvoker abstractClusterInvoker = (AbstractClusterInvoker) proxy;
            Class dubboInterface = abstractClusterInvoker.getInterface();

            String serviceSimpleName = dubboInterface.getSimpleName();
            String serviceName = dubboInterface.getName();
            String methodName = RpcUtils.getMethodName(invocation);

            if ("com.alibaba.dubbo.monitor.MonitorService".equalsIgnoreCase(serviceName)) {
                return invoker.invoke(args);
            }

            // cluster invoker 只能发生在客户端
            Span span = tracer.nextSpan();

            // 在集群阶段加span主要是为了方为无节点的情况，以及进一步比服务发现更早加入追踪。
            // 具体的span信息有待具体的节点invoker确定后补充上来。除外是无节点的情况。
            if (!span.isNoop()) {
                //只做一个简单的开启，其他操作都等首个节点invoker来补充
                span.start();
            }

            String currentEnv = EnvironmentUtils.getCurrentEnv();

            Map<String, String> propagationExtraMap = null;

            try (Tracer.SpanInScope spanScope = tracer.withSpanInScope(span)) {
                ContextHolder.get().setShareSpan(span);
                PropagationUtils.setOriginEnvIfNotExists(span.context(), currentEnv);
                propagationExtraMap = SpanUtils.getAllPropagation();
                return invoker.invoke(args);
            } catch (Throwable e) {

                // 记录异常。如果node invoker已经mark过，就跳过。
                if (!ContextHolder.get().isShareSpanMerged()) {

                    // 具体的span信息有待具体的节点invoker确定后补充上来。除外是无节点的情况。
                    // 解决典型的no provider问题。
                    span.kind(Span.Kind.CLIENT);
                    span.name(serviceSimpleName + "." + method);
                    SpanUtils.safeTag(span, "clientEnv", currentEnv);
                    SpanUtils.safeTag(span, "args", TraceJsonUtils.toJson(invocation.getArguments()));
                    SpanUtils.tagPropagationInfos(span, propagationExtraMap);

                    SpanUtils.tagErrorMark(span);
                    SpanUtils.tagError(span, e);
                }

                throw e;

            } finally {
                ContextHolder.reset();
                span.finish();
            }

        }

    }

}
