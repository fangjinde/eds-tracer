package com.netease.edu.eds.trace.instrument.redis;/**
                                                    * Created by hzfjd on 18/4/26.
                                                    */

import brave.Span;
import brave.Tracer;
import com.netease.edu.eds.trace.constants.SpanType;
import com.netease.edu.eds.trace.spi.TraceAgentInstrumetation;
import com.netease.edu.eds.trace.support.DefaultAgentBuilderListener;
import com.netease.edu.eds.trace.support.SpringBeanFactorySupport;
import com.netease.edu.eds.trace.utils.SpanUtils;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.*;
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
                                                                                                                                  javaModule) -> builder.method(not(namedIgnoreCase("getOperations")).and(isOverriddenFrom(ValueOperations.class))).intercept(MethodDelegation.to(TraceInterceptor.class))).with(DefaultAgentBuilderListener.getInstance()).installOn(inst);

        new AgentBuilder.Default().type(namedIgnoreCase("org.springframework.data.redis.core.DefaultHashOperations")).transform((builder,
                                                                                                                                 typeDescription,
                                                                                                                                 classloader,
                                                                                                                                 javaModule) -> builder.method(not(namedIgnoreCase("getOperations")).and(isOverriddenFrom(HashOperations.class))).intercept(MethodDelegation.to(TraceInterceptor.class))).with(DefaultAgentBuilderListener.getInstance()).installOn(inst);

        new AgentBuilder.Default().type(namedIgnoreCase("org.springframework.data.redis.core.DefaultListOperations")).transform((builder,
                                                                                                                                 typeDescription,
                                                                                                                                 classloader,
                                                                                                                                 javaModule) -> builder.method(not(namedIgnoreCase("getOperations")).and(isOverriddenFrom(ListOperations.class))).intercept(MethodDelegation.to(TraceInterceptor.class))).with(DefaultAgentBuilderListener.getInstance()).installOn(inst);

        new AgentBuilder.Default().type(namedIgnoreCase("org.springframework.data.redis.core.DefaultSetOperations")).transform((builder,
                                                                                                                                typeDescription,
                                                                                                                                classloader,
                                                                                                                                javaModule) -> builder.method(not(namedIgnoreCase("getOperations")).and(isOverriddenFrom(SetOperations.class))).intercept(MethodDelegation.to(TraceInterceptor.class))).with(DefaultAgentBuilderListener.getInstance()).installOn(inst);

        new AgentBuilder.Default().type(namedIgnoreCase("org.springframework.data.redis.core.DefaultZSetOperations")).transform((builder,
                                                                                                                                 typeDescription,
                                                                                                                                 classloader,
                                                                                                                                 javaModule) -> builder.method(not(namedIgnoreCase("getOperations")).and(isOverriddenFrom(ZSetOperations.class))).intercept(MethodDelegation.to(TraceInterceptor.class))).with(DefaultAgentBuilderListener.getInstance()).installOn(inst);

    }

    public static class TraceInterceptor {

        static String KEY_TEST = "keyTest";

        @RuntimeType
        public static Object around(@AllArguments Object[] args, @This Object proxy, @Origin Method method,

                                    @SuperCall Callable<Object> callable) {

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
                addNamespaceTag(proxy, span);
                SpanUtils.safeTagArgs(span, args);
                span.start();
            }

            try (Tracer.SpanInScope spanInScope = redisTracing.tracing().tracer().withSpanInScope(span)) {
                RedisTraceContext.setSpan(span);
                Object ret = callable.call();
                SpanUtils.safeTagReturn(span, ret);
                return ret;
            } catch (Throwable e) {
                SpanUtils.safeTagError(span, e);
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
                } else if (proxy instanceof HashOperations) {
                    redisSerializer = ((HashOperations) proxy).getOperations().getKeySerializer();
                } else if (proxy instanceof ListOperations) {
                    redisSerializer = ((ListOperations) proxy).getOperations().getKeySerializer();
                } else if (proxy instanceof SetOperations) {
                    redisSerializer = ((SetOperations) proxy).getOperations().getKeySerializer();
                } else if (proxy instanceof ZSetOperations) {
                    redisSerializer = ((ZSetOperations) proxy).getOperations().getKeySerializer();
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
                            String namespace = withNameSpaceKeyString.substring(0,
                                                                                withNameSpaceKeyString.indexOf(KEY_TEST));
                            if (namespace != null && namespace.length() > 0) {
                                SpanUtils.safeTag(span, "namespace", namespace);
                            }
                        }
                    }
                }
            }
        }

    }
}
