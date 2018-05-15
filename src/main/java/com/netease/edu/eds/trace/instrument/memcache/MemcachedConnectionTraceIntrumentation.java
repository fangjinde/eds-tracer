package com.netease.edu.eds.trace.instrument.memcache;

import brave.Span;
import com.netease.edu.eds.trace.spi.TraceAgentInstrumetation;
import com.netease.edu.eds.trace.support.DefaultAgentBuilderListener;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.spy.memcached.MemcachedNode;
import net.spy.memcached.ops.Operation;

import java.lang.instrument.Instrumentation;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.Callable;

import static net.bytebuddy.matcher.ElementMatchers.namedIgnoreCase;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

/**
 * @author hzfjd
 * @create 18/5/15
 **/
public class MemcachedConnectionTraceIntrumentation implements TraceAgentInstrumetation {

    @Override
    public void premain(Map<String, String> props, Instrumentation inst) {
        new AgentBuilder.Default().type(namedIgnoreCase("net.spy.memcached.MemcachedConnection")).transform((builder,
                                                                                                             typeDescription,
                                                                                                             classloader,
                                                                                                             javaModule) -> builder.method(namedIgnoreCase("addOperations").or(namedIgnoreCase("addOperation").and(takesArgument(1,
                                                                                                                                                                                                                                 MemcachedNode.class)))).intercept(MethodDelegation.to(TraceInterceptor.class))).with(DefaultAgentBuilderListener.getInstance()).installOn(inst);

    }

    public static class TraceInterceptor {

        public static void addOperation(@Argument(0) MemcachedNode node, @Argument(1) Operation o,
                                        @SuperCall Callable callable) {
            try {

                Span span = MemcacheTraceContext.currentSpan();
                if (span != null && !span.isNoop() && node != null) {
                    SocketAddress socketAddress = node.getSocketAddress();
                    SocketAddress socketAddress2 = node.getChannel().getRemoteAddress();
                    SocketAddress socketAddress3 = node.getChannel().getLocalAddress();
                    // span.remoteEndpoint(Endpoint.newBuilder().ip(node.getSocketAddress()).build());
                }
                callable.call();
            } catch (Exception e) {
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                } else {
                    throw new RuntimeException("error while tracing Memcached Connection.addOperation.", e);
                }
            }
        }

        public static void addOperations(@Argument(0) Map<MemcachedNode, Operation> ops, @SuperCall Callable callable) {
            try {

                Span span = MemcacheTraceContext.currentSpan();
                if (span != null && !span.isNoop() && ops != null && !ops.isEmpty()) {
                    // SocketAddress socketAddress = node.getSocketAddress();
                    // SocketAddress socketAddress2 = node.getChannel().getRemoteAddress();
                    // SocketAddress socketAddress3 = node.getChannel().getLocalAddress();
                    // span.remoteEndpoint(Endpoint.newBuilder().ip(node.getSocketAddress()).build());
                }
                callable.call();
            } catch (Exception e) {
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                } else {
                    throw new RuntimeException("error while tracing Memcached Connection.addOperations.", e);
                }
            }
        }
    }

}
