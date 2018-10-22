package com.netease.edu.eds.trace.instrument.transaction.message;

import brave.Span;
import brave.Tracer;
import brave.Tracing;
import brave.propagation.Propagation;
import brave.propagation.TraceContext;
import com.netease.edu.eds.trace.constants.SpanType;
import com.netease.edu.eds.trace.core.Invoker;
import com.netease.edu.eds.trace.support.AbstractTraceAgentInstrumetation;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bind.annotation.*;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.utility.JavaModule;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.lang.reflect.Method;
import java.util.Map;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * @author hzfjd
 * @create 18/10/22
 **/
public class RabbitTemplateForTransactionMessageInstrument extends AbstractTraceAgentInstrumetation {

    private static final String SEND_METHOD_NAME = "send";

    @Override
    protected ElementMatcher.Junction defineTypeMatcher(Map<String, String> props) {
        return namedIgnoreCase("org.springframework.amqp.rabbit.core.RabbitTemplate");
    }

    @Override
    protected ElementMatcher.Junction defineMethodMatcher(Map<String, String> props, TypeDescription typeDescription,
                                                          ClassLoader classLoader, JavaModule module) {
        // public void send(String routingKey, Message message) throws AmqpException
        ElementMatcher.Junction send2 = isDeclaredBy(typeDescription).and(namedIgnoreCase(SEND_METHOD_NAME)).and(takesArguments(2)).and(isPublic());
        return send2;
    }

    @Override
    protected Class defineInterceptorClass(Map<String, String> props) {
        return Interceptor.class;
    }

    public static class Interceptor {

        private static Propagation.Getter<MessageProperties, String> GETTER = (MessageProperties carrier,
                                                                               String key) -> (String) carrier.getHeaders().get(key);

        @RuntimeType
        public static Object intercept(@AllArguments Object[] args, @Morph Invoker invoker, @Origin Method method,
                                       @This Object proxy) {

            // do for "send" method
            if (method.getName().equals(SEND_METHOD_NAME)) {
                return send(args, invoker, method, proxy);
            } else {
                return invoker.invoke(args);
            }

        }

        /**
         * 因为真正的RabbitTemplate的发送，已经有比较完整的span记录。所以，只做必须的记录，即提取propagation，放到context中。
         * 
         * @param args
         * @param invoker
         * @param method
         * @param proxy
         * @return
         */
        private static Object send(Object[] args, Invoker invoker, Method method, Object proxy) {
            // recheck和reconsume没有特别合适的拦截方法（要求拥有获取carrier的入参），只能在外层调用函数打标。然后根据这个标记进行识别。
            if (!TransactionMessageRestoreMethodContext.inRetoreInAll()) {
                return invoker.invoke(args);
            }

            Tracing tracing = Tracing.current();
            Tracer tracer = null;
            if (tracing != null) {
                tracer = tracing.tracer();
            }
            if (tracer == null) {
                return invoker.invoke(args);
            }

            Object arg1 = args[1];
            Message message = null;
            MessageProperties messageProperties = null;

            if (arg1 instanceof Message) {
                message = (Message) arg1;
                messageProperties = message.getMessageProperties();
            }

            if (messageProperties == null) {
                return invoker.invoke(args);
            }

            TraceContext.Extractor extractor = tracing.propagation().extractor(GETTER);
            Span span = tracer.nextSpan(extractor.extract(messageProperties));

            if (!span.isNoop()) {
                span.name(TransactionMessageRestoreMethodContext.getMethodName()).kind(Span.Kind.CLIENT).tag(SpanType.TAG_KEY,
                                                                                                             SpanType.TRANSACTION_MESSAGE).start();
            }

            try (Tracer.SpanInScope spanInScope = tracer.withSpanInScope(span)) {
                Object result = invoker.invoke(args);
                return result;
            } finally {
                span.finish();
            }

        }
    }
}
