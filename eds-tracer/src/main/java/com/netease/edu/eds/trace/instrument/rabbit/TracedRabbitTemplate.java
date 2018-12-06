package com.netease.edu.eds.trace.instrument.rabbit;/**
                                                     * Created by hzfjd on 18/4/12.
                                                     */

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.support.CorrelationData;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;

import com.netease.edu.eds.trace.utils.SpanUtils;
import com.rabbitmq.client.Channel;

import brave.Span;
import brave.Tracer;
import brave.propagation.Propagation;
import brave.propagation.TraceContext;
import zipkin2.Endpoint;

/**
 * @See RabbitTemplateInstrumentation
 * @author hzfjd
 * @create 18/4/12
 */
@Deprecated
public class TracedRabbitTemplate extends RabbitTemplate {

    public TracedRabbitTemplate() {
        super();
    }

    public TracedRabbitTemplate(ConnectionFactory connectionFactory) {
        super(connectionFactory);
    }

    static final Propagation.Setter<MessageProperties, String> SETTER = new Propagation.Setter<MessageProperties, String>() {

                                                                          @Override
                                                                          public void put(MessageProperties carrier,
                                                                                          String key, String value) {
                                                                              carrier.setHeader(key, value);
                                                                          }

                                                                          @Override
                                                                          public String toString() {
                                                                              return "MessageProperties::setHeader";
                                                                          }
                                                                      };

    private RabbitTracing                                      rabbitTracing;
    private TraceContext.Injector<MessageProperties>           injector;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        super.setBeanFactory(beanFactory);
        rabbitTracing = beanFactory.getBean(RabbitTracing.class);
        injector = rabbitTracing.tracing().propagation().injector(SETTER);
    }

    @Override
    public void doSend(Channel channel, String exchange, String routingKey, Message message, boolean mandatory,
                          CorrelationData correlationData) throws Exception {

        Tracer tracer = rabbitTracing.tracing().tracer();
        Span span = tracer.nextSpan().kind(Span.Kind.PRODUCER).name("publish");
        if (!span.isNoop()) {

            RabbitTracing.tagSendMessageInfo(span, exchange, routingKey);
            RabbitTracing.tagMessagePayload(span, message.toString());

            Endpoint.Builder builder = Endpoint.newBuilder();
            if (rabbitTracing.remoteServiceName() != null) {
                builder.serviceName(rabbitTracing.remoteServiceName());

            }
            if (channel.getConnection().getAddress() != null) {
                builder.parseIp(channel.getConnection().getAddress());
            }
            span.remoteEndpoint(builder.build());

            span.start();

        }

        try (Tracer.SpanInScope spanInScope = tracer.withSpanInScope(span)) {
            injector.inject(span.context(), message.getMessageProperties());
            super.doSend(channel, exchange, routingKey, message, mandatory, correlationData);
        } catch (Exception e) {
            SpanUtils.tagErrorMark(span);
            SpanUtils.tagError(span, e);
            throw e;
        } finally {

            span.finish();
        }

    }

}
