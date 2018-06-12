package com.netease.edu.eds.trace.instrument.rabbit;/**
                                                     * Created by hzfjd on 18/4/12.
                                                     */

import org.springframework.amqp.core.MessageProperties;

import com.google.auto.value.AutoValue;

import brave.Span;
import brave.Tracing;

/**
 * @author hzfjd
 * @create 18/4/12
 */
@AutoValue
public abstract class RabbitTracing {

    static final String RABBIT_EXCHANGE = "rabbit.exchange", RABBIT_ROUTING_KEY = "rabbit.routing_key",
            RABBIT_QUEUE = "rabbit.queue", RABBIT_PAYLOAD = "rabbit.payload";

    public abstract String remoteServiceName();

    public abstract Tracing tracing();

    public static Builder newBuilder() {
        return new AutoValue_RabbitTracing.Builder().remoteServiceName("rabbit");
    }

    public abstract Builder toBuilder();

    RabbitTracing() {

    }

    @AutoValue.Builder
    public static abstract class Builder {

        public abstract Builder remoteServiceName(String remoteServiceName);

        public abstract Builder tracing(Tracing tracing);

        public abstract RabbitTracing build();

        Builder() {

        }
    }

    public static void tagSendMessageInfo(Span span, String exchange, String routingKey) {
        maybeTag(span, RabbitTracing.RABBIT_EXCHANGE, exchange);
        maybeTag(span, RabbitTracing.RABBIT_ROUTING_KEY, routingKey);
    }

    public static void tagReceivedMessageProperties(Span span, MessageProperties messageProperties) {
        maybeTag(span, RabbitTracing.RABBIT_EXCHANGE, messageProperties.getReceivedExchange());
        maybeTag(span, RabbitTracing.RABBIT_ROUTING_KEY, messageProperties.getReceivedRoutingKey());
        maybeTag(span, RabbitTracing.RABBIT_QUEUE, messageProperties.getConsumerQueue());
    }

    public static void tagMessagePayload(Span span, String payload) {
        maybeTag(span, RabbitTracing.RABBIT_PAYLOAD, payload);
    }

    public static void maybeTag(Span span, String tag, String value) {
        if (value != null) {
            span.tag(tag, value);
        }
    }

    public static void tagErrorSpan(Span span, Throwable t) {
        String errorMessage = t.getMessage();
        if (errorMessage == null) errorMessage = t.getClass().getSimpleName();
        span.tag("error", errorMessage);
    }

}
