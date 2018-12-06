package com.netease.edu.eds.trace.instrument.redis;

import brave.Span;
import brave.Tracer;
import com.netease.edu.eds.trace.constants.SpanType;
import com.netease.edu.eds.trace.spi.TraceAgentInstrumetation;
import com.netease.edu.eds.trace.support.DefaultAgentBuilderListener;
import com.netease.edu.eds.trace.support.SpringBeanFactorySupport;
import com.netease.edu.eds.trace.utils.SpanUtils;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import redis.clients.jedis.JedisCommands;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.Callable;

import static net.bytebuddy.matcher.ElementMatchers.isOverriddenFrom;
import static net.bytebuddy.matcher.ElementMatchers.namedIgnoreCase;

/**
 * @author hzfjd
 * @create 18/5/14
 **/
public class JedisClientIntrumentation implements TraceAgentInstrumetation {

    @Override
    public void premain(Map<String, String> props, Instrumentation inst) {
        new AgentBuilder.Default().type(namedIgnoreCase("redis.clients.jedis.Jedis")).transform((builder,
                                                                                                 typeDescription,
                                                                                                 classloader,
                                                                                                 javaModule) -> builder.method((isOverriddenFrom(JedisCommands.class))).intercept(MethodDelegation.to(TraceInterceptor.class))).with(DefaultAgentBuilderListener.getInstance()).installOn(inst);
    }

    public static class TraceInterceptor {

        @RuntimeType
        public static Object around(@AllArguments Object[] args, @SuperCall Callable<Object> callable,
                                    @Origin Method method) {

            RedisTracing redisTracing = SpringBeanFactorySupport.getBean(RedisTracing.class);

            if (redisTracing == null) {
                try {
                    return callable.call();
                } catch (Throwable e) {
                    if (e instanceof RuntimeException) {
                        throw (RuntimeException) e;
                    } else {
                        throw new RuntimeException("unknown redis operation exception", e);
                    }

                }

            }
            Span span = redisTracing.tracing().tracer().nextSpan();

            SpanUtils.safeTag(span, SpanType.TAG_KEY, SpanType.REDIS);

            if (!span.isNoop()) {
                span.kind(Span.Kind.CLIENT).name(method.getDeclaringClass().getSimpleName() + "." + method.getName());
                SpanUtils.safeTagArgs(span,args);
                span.start();
            }

            try (Tracer.SpanInScope spanInScope = redisTracing.tracing().tracer().withSpanInScope(span)) {
                RedisTraceContext.setSpan(span);
                Object ret = callable.call();
                SpanUtils.safeTagReturn(span,ret);
                return ret;
            } catch (Throwable e) {
                SpanUtils.safeTagError(span,e);
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                } else {
                    throw new RuntimeException("unknown redis operation exception", e);
                }
            } finally {
                RedisTraceContext.setSpan(null);
                span.finish();
            }

        }
    }
}
