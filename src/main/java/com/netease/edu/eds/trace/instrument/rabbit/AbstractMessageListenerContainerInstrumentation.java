package com.netease.edu.eds.trace.instrument.rabbit;

import brave.Span;
import brave.Tracer;
import brave.propagation.Propagation;
import brave.propagation.TraceContext;
import brave.propagation.TraceContextOrSamplingFlags;
import com.netease.edu.eds.trace.constants.SpanType;
import com.netease.edu.eds.trace.core.Invoker;
import com.netease.edu.eds.trace.spi.TraceAgentInstrumetation;
import com.netease.edu.eds.trace.support.AgentSupport;
import com.netease.edu.eds.trace.support.DefaultAgentBuilderListener;
import com.netease.edu.eds.trace.support.SpringBeanFactorySupport;
import com.netease.edu.eds.trace.utils.SpanUtils;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bind.annotation.*;
import net.bytebuddy.matcher.ElementMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import zipkin2.Endpoint;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.Map;

import static brave.Span.Kind.CONSUMER;
import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * @author hzfjd
 * @create 18/7/10
 **/
public class AbstractMessageListenerContainerInstrumentation implements TraceAgentInstrumetation {

    private ElementMatcher.Junction getMatcher(TypeDescription typeDescription) {
        ElementMatcher.Junction matcher1 = isDeclaredBy(typeDescription).and(namedIgnoreCase("invokeListener")).and(takesArguments(2));
        return matcher1;
    }

    @Override
    public void premain(Map<String, String> props, Instrumentation inst) {

        new AgentBuilder.Default().type(namedIgnoreCase("org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer")).transform((builder,
                                                                                                                                                 typeDescription,
                                                                                                                                                 classloader,
                                                                                                                                                 javaModule) -> builder.method(getMatcher(typeDescription)).intercept(AgentSupport.getInvokerMethodDelegationCustomer().to(Interceptor.class))).with(DefaultAgentBuilderListener.getInstance()).installOn(inst);

    }

    public static class Interceptor {

        private static Logger logger = LoggerFactory.getLogger(Interceptor.class);

        @RuntimeType
        public static Object intercept(@AllArguments Object[] args, @Morph Invoker invoker, @Origin Method method,
                                       @This Object proxy) throws Exception {

            // do for "invokeListener" method
            RabbitTracing rabbitTracing = SpringBeanFactorySupport.getBean(RabbitTracing.class);
            if (rabbitTracing == null) {
                return invoker.invoke(args);
            }

            Channel channel = (Channel) args[0];
            Message message = (Message) args[1];

            TraceContext.Extractor<MessageProperties> extractor = rabbitTracing.tracing().propagation().extractor(GETTER);
            TraceContextOrSamplingFlags extracted = extractTraceContextAndRemoveHeaders(message, rabbitTracing,
                                                                                        extractor);

            Tracer tracer = rabbitTracing.tracing().tracer();
            Span consumerSpan = tracer.nextSpan(extracted).kind(CONSUMER).name("on-message");
            SpanUtils.safeTag(consumerSpan, SpanType.TAG_KEY, SpanType.RABBIT);

            if (!consumerSpan.isNoop()) {
                consumerSpan.start();
                RabbitTracing.tagReceivedMessageProperties(consumerSpan, message.getMessageProperties());
                RabbitTracing.tagMessagePayload(consumerSpan, message.toString());
                Endpoint.Builder builder = Endpoint.newBuilder();
                if (rabbitTracing.remoteServiceName() != null) {
                    builder.serviceName(rabbitTracing.remoteServiceName());

                }

                // 需要增加try catch处理。否则可能会导致连接异常。后续可以深入研究下。
                try {
                    Connection connection = channel.getConnection();
                    if (connection != null) {
                        InetAddress inetAddress = connection.getAddress();
                        if (inetAddress != null) {
                            builder.parseIp(connection.getAddress());
                        }
                    }
                } catch (Exception e) {
                    logger.error("parse Address from rabbit channel failed.", e);
                }

                consumerSpan.remoteEndpoint(builder.build());

            }

            try (Tracer.SpanInScope ws = tracer.withSpanInScope(consumerSpan)) {
                SpanUtils.tagPropagationInfos(consumerSpan);
                return invoker.invoke(args);
            } catch (Throwable t) {
                SpanUtils.tagErrorMark(consumerSpan);
                SpanUtils.tagError(consumerSpan, t);
                throw t;
            } finally {
                consumerSpan.finish();
            }
        }

        static TraceContextOrSamplingFlags extractTraceContextAndRemoveHeaders(Message message,
                                                                               RabbitTracing rabbitTracing,
                                                                               TraceContext.Extractor<MessageProperties> extractor) {
            MessageProperties messageProperties = message.getMessageProperties();
            TraceContextOrSamplingFlags extracted = extractor.extract(messageProperties);
            Map<String, Object> headers = messageProperties.getHeaders();
            for (String key : rabbitTracing.tracing().propagation().keys()) {
                headers.remove(key);
            }
            return extracted;
        }

        static final Propagation.Getter<MessageProperties, String> GETTER = new Propagation.Getter<MessageProperties, String>() {

            @Override
            public String get(MessageProperties carrier, String key) {
                return (String) carrier.getHeaders().get(key);
            }

            @Override
            public String toString() {
                return "MessageProperties::getHeader";
            }
        };
    }
}
