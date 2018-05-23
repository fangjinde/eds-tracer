package com.netease.edu.eds.trace.instrument.redis;

import brave.Span;
import brave.Tracer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netease.edu.eds.trace.spi.TraceAgentInstrumetation;
import com.netease.edu.eds.trace.support.DefaultAgentBuilderListener;
import com.netease.edu.eds.trace.support.SpringBeanFactorySupport;
import com.netease.edu.eds.trace.utils.ExceptionStringUtils;
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

        static ObjectMapper objectMapper = new ObjectMapper();

        @RuntimeType
        public static Object around(@AllArguments Object[] args, @SuperCall Callable<Object> callable,
                                    @Origin Method method) {

            RedisTracing redisTracing = SpringBeanFactorySupport.getBean(RedisTracing.class);

            if (redisTracing == null) {
                try {
                    return callable.call();
                } catch (Exception e) {
                    if (e instanceof RuntimeException) {
                        throw (RuntimeException) e;

                    } else {
                        throw new RuntimeException("unknown redis operation exception", e);
                    }

                }

            }
            Span span = redisTracing.tracing().tracer().nextSpan();
            if (!span.isNoop()) {
                span.kind(Span.Kind.CLIENT).name(method.getDeclaringClass().getSimpleName() + "." + method.getName());

                try {
                    String argsJson = objectMapper.writeValueAsString(args);
                    span.tag("args", argsJson);
                } catch (JsonProcessingException e) {

                }
                span.start();
            }

            try (Tracer.SpanInScope spanInScope = redisTracing.tracing().tracer().withSpanInScope(span)) {

                RedisTraceContext.setSpan(span);

                Object ret = callable.call();
                try {
                    String retJson = new ObjectMapper().writeValueAsString(ret);
                    span.tag("return", retJson);
                } catch (JsonProcessingException e) {

                }
                return ret;
            } catch (Exception e) {
                span.tag("redis_error", ExceptionStringUtils.getStackTraceString(e));
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
