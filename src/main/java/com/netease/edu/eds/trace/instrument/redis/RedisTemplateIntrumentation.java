package com.netease.edu.eds.trace.instrument.redis;/**
                                                    * Created by hzfjd on 18/4/26.
                                                    */

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
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.Callable;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * @author hzfjd
 * @create 18/4/26
 */
public class RedisTemplateIntrumentation implements TraceAgentInstrumetation {

    @Override
    public void premain(Map<String, String> props, Instrumentation inst) {

        new AgentBuilder.Default().type(namedIgnoreCase("org.springframework.data.redis.core.DefaultValueOperations")).transform((builder,
                                                                                                                                  typeDescription,
                                                                                                                                  classloader,
                                                                                                                                  javaModule) -> builder.method(namedIgnoreCase("set").and(takesArguments(2)).and(isDeclaredBy(typeDescription))).intercept(MethodDelegation.to(TraceInterceptor.class))).with(DefaultAgentBuilderListener.getInstance()).installOn(inst);

    }

    public static class TraceInterceptor {

        static ObjectMapper objectMapper = new ObjectMapper();
        static String       KEY_TEST     = "keyTest";

        public static Object around(@AllArguments Object[] args, @This Object proxy, @Origin Method method,

                                    @SuperCall Callable<Object> callable) {

            RedisTracing redisTracing = null;
            BeanFactory beanFactory = SpringBeanFactorySupport.getBeanFactory();
            if (beanFactory != null) {
                redisTracing = beanFactory.getBean(RedisTracing.class);
            }

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
                // span.remoteEndpoint(Endpoint.newBuilder().ip(uri.getHost()).port(uri.getPort()).build());
                addNamespaceTag(proxy, span);

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

        private static void addNamespaceTag(Object proxy, Span span) {
            try {
                RedisSerializer redisSerializer = null;
                if (proxy instanceof ValueOperations) {
                    redisSerializer = ((ValueOperations) proxy).getOperations().getKeySerializer();
                } else if (proxy instanceof BoundValueOperations) {
                    redisSerializer = ((BoundValueOperations) proxy).getOperations().getKeySerializer();
                } else if (proxy instanceof HashOperations) {
                    redisSerializer = ((HashOperations) proxy).getOperations().getKeySerializer();
                } else if (proxy instanceof BoundHashOperations) {
                    redisSerializer = ((BoundHashOperations) proxy).getOperations().getKeySerializer();
                } else if (proxy instanceof ZSetOperations) {
                    redisSerializer = ((ZSetOperations) proxy).getOperations().getKeySerializer();
                } else if (proxy instanceof BoundZSetOperations) {
                    redisSerializer = ((BoundZSetOperations) proxy).getOperations().getKeySerializer();
                }

                addNamespaceTag(redisSerializer, span);
            } catch (Exception e) {

            }

        }

        private static void addNamespaceTag(RedisSerializer redisSerializer, Span span) {
            if (redisSerializer != null) {
                byte[] serializedKey = redisSerializer.serialize(KEY_TEST);
                if (serializedKey != null) {
                    Object keyObject = redisSerializer.deserialize(serializedKey);
                    if (keyObject instanceof String) {
                        String withNameSpaceKeyString = (String) keyObject;
                        if (withNameSpaceKeyString.endsWith(KEY_TEST)) {
                            String namespace = withNameSpaceKeyString.substring(withNameSpaceKeyString.indexOf(KEY_TEST));
                            if (namespace != null && namespace.length() > 0) {
                                span.tag("namespace", namespace);
                            }
                        }
                    }
                }
            }
        }

    }
}
