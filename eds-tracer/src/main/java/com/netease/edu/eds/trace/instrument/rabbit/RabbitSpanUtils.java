package com.netease.edu.eds.trace.instrument.rabbit;

import brave.Span;
import org.springframework.amqp.core.MessageProperties;

/**
 * @author hzfjd
 * @create 19/1/23
 **/
public class RabbitSpanUtils {

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
}
