package com.netease.edu.eds.trace.instrument.memcache;

import brave.Span;
import com.netease.edu.eds.trace.spi.TraceAgentInstrumetation;
import com.netease.edu.eds.trace.support.DefaultAgentBuilderListener;
import com.netease.edu.eds.trace.utils.JsonUtils;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.spy.memcached.MemcachedNode;
import net.spy.memcached.ops.Operation;
import zipkin2.Endpoint;

import java.lang.instrument.Instrumentation;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
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
                                                                                                             javaModule) -> builder.method(namedIgnoreCase("addOperations").or(namedIgnoreCase("addOperation").and(takesArgument(0,
                                                                                                                                                                                                                                 MemcachedNode.class)))).intercept(MethodDelegation.to(TraceInterceptor.class))).with(DefaultAgentBuilderListener.getInstance()).installOn(inst);

    }

    public static class TraceInterceptor {


        public static void addOperation(@Argument(0) MemcachedNode node, @Argument(1) Operation o,
                                        @SuperCall Callable<Void> callable) {
            try {

                Span span = MemcacheTraceContext.currentSpan();
                if (span != null && !span.isNoop() && node != null) {
                    addEndpointTag(span, node);
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

        public static void addOperations(@Argument(0) Map<MemcachedNode, Operation> ops,
                                         @SuperCall Callable<Void> callable) {
            try {

                Span span = MemcacheTraceContext.currentSpan();
                if (span != null && !span.isNoop() && ops != null && !ops.isEmpty()) {
                    MemcachedNode first = null;
                    int index = 0;
                    List<String> hostLists = new ArrayList<>();

                    for (Map.Entry<MemcachedNode, Operation> entry : ops.entrySet()) {
                        if (0 == index) {
                            addEndpointTag(span, entry.getKey());
                        }
                        InetAddress inetAddress = getINetAddressFromNode(entry.getKey());
                        if (inetAddress != null) {
                            hostLists.add(inetAddress.getHostAddress() + ":" + getPortFromNode(entry.getKey()));
                        }

                        index++;

                    }

                    if (index > 0) {
                        span.tag("AllNodes", JsonUtils.toJson(hostLists));
                    }

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

        // private static void addNodestag(Span span,)

        private static void addEndpointTag(Span span, MemcachedNode memcachedNode) {

            InetAddress inetAddress = getINetAddressFromNode(memcachedNode);
            if (null != inetAddress) {
                span.remoteEndpoint(Endpoint.newBuilder().ip(inetAddress).port(getPortFromNode(memcachedNode)).build());
            }
        }

        private static InetAddress getINetAddressFromNode(MemcachedNode memcachedNode) {
            SocketAddress socketAddress = memcachedNode.getSocketAddress();
            if (socketAddress instanceof InetSocketAddress) {
                InetSocketAddress inetSocketAddress = (InetSocketAddress) socketAddress;
                return inetSocketAddress.getAddress();
            }
            return null;
        }

        private static int getPortFromNode(MemcachedNode memcachedNode) {
            SocketAddress socketAddress = memcachedNode.getSocketAddress();
            if (socketAddress instanceof InetSocketAddress) {
                InetSocketAddress inetSocketAddress = (InetSocketAddress) socketAddress;
                return inetSocketAddress.getPort();
            }
            return -1;
        }
    }

}
