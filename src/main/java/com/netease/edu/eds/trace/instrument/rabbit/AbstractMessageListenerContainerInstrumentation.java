package com.netease.edu.eds.trace.instrument.rabbit;

import brave.Span;
import brave.Tracer;
import brave.propagation.Propagation;
import brave.propagation.TraceContext;
import brave.propagation.TraceContextOrSamplingFlags;
import com.netease.edu.eds.trace.core.Invoker;
import com.netease.edu.eds.trace.spi.TraceAgentInstrumetation;
import com.netease.edu.eds.trace.support.AgentSupport;
import com.netease.edu.eds.trace.support.DefaultAgentBuilderListener;
import com.netease.edu.eds.trace.support.SpringBeanFactorySupport;
import com.rabbitmq.client.Channel;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Morph;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import zipkin2.Endpoint;

import java.lang.instrument.Instrumentation;
import java.util.Map;

import static brave.Span.Kind.CONSUMER;
import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * @author hzfjd
 * @create 18/7/10
 **/
public class AbstractMessageListenerContainerInstrumentation implements TraceAgentInstrumetation {

    @Override
    public void premain(Map<String, String> props, Instrumentation inst) {
        new AgentBuilder.Default().type(namedIgnoreCase("org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer")).transform((builder,
                                                                                                                                                 typeDescription,
                                                                                                                                                 classloader,
                                                                                                                                                 javaModule) -> builder.method(isOverriddenFrom(typeDescription).and(namedIgnoreCase("invokeListener")).and(takesArguments(2))).intercept(AgentSupport.getInvokerMethodDelegationCustomer().to(TraceInterceptor.class))).with(DefaultAgentBuilderListener.getInstance()).installOn(inst);

    }

    public static class TraceInterceptor {

        @RuntimeType
        public static Object invokeListener(@AllArguments Object[] args, @Morph Invoker invoker) throws Exception {

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

            if (!consumerSpan.isNoop()) {
                consumerSpan.start();
                RabbitTracing.tagReceivedMessageProperties(consumerSpan, message.getMessageProperties());
                RabbitTracing.tagMessagePayload(consumerSpan, message.toString());
                Endpoint.Builder builder = Endpoint.newBuilder();
                if (rabbitTracing.remoteServiceName() != null) {
                    builder.serviceName(rabbitTracing.remoteServiceName());

                }

                if (channel.getConnection().getAddress() != null) {
                    builder.parseIp(channel.getConnection().getAddress());
                }

                consumerSpan.remoteEndpoint(builder.build());

            }

            try (Tracer.SpanInScope ws = tracer.withSpanInScope(consumerSpan)) {
                return invoker.invoke(args);
            } catch (Throwable t) {
                RabbitTracing.tagErrorSpan(consumerSpan, t);
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
