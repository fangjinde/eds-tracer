package com.netease.edu.eds.trace.instrument.memcache;

import brave.Span;
import brave.Tracer;
import com.netease.edu.eds.trace.constants.SpanType;
import com.netease.edu.eds.trace.spi.TraceAgentInstrumetation;
import com.netease.edu.eds.trace.support.DefaultAgentBuilderListener;
import com.netease.edu.eds.trace.support.SpringBeanFactorySupport;
import com.netease.edu.eds.trace.utils.SpanUtils;
import com.netease.edu.eds.trace.utils.TraceJsonUtils;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.Callable;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * @author hzfjd
 * @create 18/5/14
 **/
public class MemcacheTraceIntrumentation implements TraceAgentInstrumetation {

    @Override
    public void premain(Map<String, String> props, Instrumentation inst) {
        new AgentBuilder.Default().type(namedIgnoreCase("net.spy.memcached.MemcachedClient")).transform((builder,
                                                                                                         typeDescription,
                                                                                                         classloader,
                                                                                                         javaModule) -> builder.method((isDeclaredBy(typeDescription).and(isPublic()).and(not(namedIgnoreCase("getAvailableServers").or(namedIgnoreCase("getUnavailableServers")).or(namedIgnoreCase("getTranscoder")).or(namedIgnoreCase("getNodeLocator")).or(namedIgnoreCase("getVersions")).or(namedIgnoreCase("getStats")).or(namedIgnoreCase("flush")).or(namedIgnoreCase("shutdown")).or(namedIgnoreCase("waitForQueues")).or(namedIgnoreCase("addObserver")).or(namedIgnoreCase("removeObserver")).or(namedIgnoreCase("listSaslMechanisms")).or(namedIgnoreCase("toString")))))).intercept(MethodDelegation.to(TraceInterceptor.class))).with(DefaultAgentBuilderListener.getInstance()).installOn(inst);

    }

    public static class TraceInterceptor {

        @RuntimeType
        public static Object around(@AllArguments Object[] args, @SuperCall Callable<Object> callable,
                                    @Origin Method method) {

            Span span = MemcacheTraceContext.currentSpan();
            // some public method is called after another public method. This should be ignored while tracing.
            if (span != null) {
                try {
                    return callable.call();
                } catch (Exception e) {
                    if (e instanceof RuntimeException) {
                        throw (RuntimeException) e;

                    } else {
                        throw new RuntimeException("unknown memcache operation exception", e);
                    }

                }
            }

            MemcacheTracing memcacheTracing = SpringBeanFactorySupport.getBean(MemcacheTracing.class);

            if (memcacheTracing == null) {
                try {
                    return callable.call();
                } catch (Exception e) {
                    if (e instanceof RuntimeException) {
                        throw (RuntimeException) e;

                    } else {
                        throw new RuntimeException("unknown memcache operation exception", e);
                    }

                }

            }

            span = memcacheTracing.tracing().tracer().nextSpan();
            SpanUtils.safeTag(span, SpanType.TAG_KEY, SpanType.MEMCACHE);

            if (!span.isNoop()) {
                span.kind(Span.Kind.CLIENT).name(method.getDeclaringClass().getSimpleName() + "." + method.getName());

                String argsJson = TraceJsonUtils.toJson(args);
                SpanUtils.safeTag(span, "args", argsJson);
                span.start();
            }

            try (Tracer.SpanInScope spanInScope = memcacheTracing.tracing().tracer().withSpanInScope(span)) {

                MemcacheTraceContext.setSpan(span);

                Object ret = callable.call();
                String retJson = TraceJsonUtils.toJson(ret);
                SpanUtils.safeTag(span, "return", retJson);
                return ret;
            } catch (Exception e) {
                SpanUtils.tagErrorMark(span);
                SpanUtils.tagError(span, e);
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                } else {
                    throw new RuntimeException("unknown memcache operation exception", e);
                }
            } finally {
                MemcacheTraceContext.setSpan(null);
                span.finish();
            }
        }
    }
}
