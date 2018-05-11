package com.netease.edu.eds.trace.instrument.redis;

import brave.Span;
import com.netease.edu.eds.trace.spi.TraceAgentInstrumetation;
import com.netease.edu.eds.trace.support.DefaultAgentBuilderListener;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;
import net.bytebuddy.matcher.ElementMatchers;
import redis.clients.jedis.Connection;
import redis.clients.jedis.Protocol;
import zipkin2.Endpoint;

import java.lang.instrument.Instrumentation;
import java.util.Map;
import java.util.concurrent.Callable;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * @author hzfjd
 * @create 18/5/11
 **/
public class JedisConnectionInstrumentation implements TraceAgentInstrumetation {

    @Override
    public void premain(Map<String, String> props, Instrumentation inst) {
        new AgentBuilder.Default().type(namedIgnoreCase("redis.clients.jedis.Connection")).transform((builder,
                                                                                                      typeDescription,
                                                                                                      classloader,
                                                                                                      javaModule) -> builder.method(namedIgnoreCase("sendCommand").and(takesArguments(2)).and(ElementMatchers.takesArgument(1,
                                                                                                                                                                                                                            byte[][].class)).and(isDeclaredBy(typeDescription))).intercept(MethodDelegation.to(TraceInterceptor.class))).with(DefaultAgentBuilderListener.getInstance()).installOn(inst);
    }

    public static class TraceInterceptor {

        protected Connection sendCommand(@SuperCall Callable<Connection> callable, @This Object proxy,
                                         final @Argument(0) Protocol.Command cmd, final @Argument(1) byte[]... args) {

            Span span = RedisTraceContext.currentSpan();
            if (span != null && !span.isNoop()) {
                if (proxy instanceof Connection) {
                    Connection connection = (Connection) proxy;
                    span.remoteEndpoint(Endpoint.newBuilder().ip(connection.getHost()).port(connection.getPort()).build());
                }
            }

            try {
                return callable.call();
            } catch (Exception e) {
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                } else {
                    throw new RuntimeException("error while tracing Jedis Connection.sendCommand.", e);
                }
            }

        }

    }
}
