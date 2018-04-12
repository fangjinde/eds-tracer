package com.netease.edu.eds.trace.instrument.rabbit;/**
 * Created by hzfjd on 18/4/12.
 */

import brave.Span;
import brave.Tracer;
import brave.propagation.Propagation;
import brave.propagation.TraceContext;
import brave.propagation.TraceContextOrSamplingFlags;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import zipkin2.Endpoint;

import java.util.Map;

import static brave.Span.Kind.CONSUMER;

/**
 * @author hzfjd
 * @create 18/4/12
 */
public class SimpleTracedMessageListenerContainer extends SimpleMessageListenerContainer {

    static final Propagation.Getter<MessageProperties, String> GETTER = new Propagation.Getter<MessageProperties, String>() {

        @Override public String get(MessageProperties carrier, String key) {
            return (String) carrier.getHeaders().get(key);
        }

        @Override public String toString() {
            return "MessageProperties::getHeader";
        }
    };

    private RabbitTracing rabbitTracing;

    private TraceContext.Extractor<MessageProperties> extractor;

    @Override protected void doInitialize() throws Exception {
        super.doInitialize();
        rabbitTracing = getApplicationContext().getBean(RabbitTracing.class);
        if (rabbitTracing != null) {
            extractor = rabbitTracing.tracing().propagation().extractor(GETTER);
        }

    }

    @Override protected void invokeListener(Channel channel, Message message) throws Exception {

        TraceContextOrSamplingFlags extracted = extractTraceContextAndRemoveHeaders(message);

        Tracer tracer = rabbitTracing.tracing().tracer();
        // named for BlockingQueueConsumer.nextMessage, which we can't currently see
        Span consumerSpan = tracer.nextSpan(extracted).kind(CONSUMER).name("on-message");
        //Span listenerSpan = tracer.newChild(consumerSpan.context()).name("on-message");

        if (!consumerSpan.isNoop()) {
            consumerSpan.start();
            RabbitTracing.tagReceivedMessageProperties(consumerSpan, message.getMessageProperties());
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
            super.invokeListener(channel, message);
        } catch (Throwable t) {
            RabbitTracing.tagErrorSpan(consumerSpan, t);
            throw t;
        } finally {
            consumerSpan.finish();
        }

    }

    TraceContextOrSamplingFlags extractTraceContextAndRemoveHeaders(Message message) {
        MessageProperties messageProperties = message.getMessageProperties();
        TraceContextOrSamplingFlags extracted = extractor.extract(messageProperties);
        Map<String, Object> headers = messageProperties.getHeaders();
        for (String key : rabbitTracing.tracing().propagation().keys()) {
            headers.remove(key);
        }
        return extracted;
    }


}
