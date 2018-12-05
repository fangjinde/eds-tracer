package com.netease.edu.eds.trace.instrument.rabbit;

import brave.Span;
import brave.Tracer;
import brave.propagation.Propagation;
import brave.propagation.TraceContext;
import brave.propagation.TraceContextOrSamplingFlags;
import com.netease.edu.eds.trace.constants.CommonTagKeys;
import com.netease.edu.eds.trace.constants.SpanType;
import com.netease.edu.eds.trace.core.Invoker;
import com.netease.edu.eds.trace.support.SpringBeanFactorySupport;
import com.netease.edu.eds.trace.utils.EnvironmentUtils;
import com.netease.edu.eds.trace.utils.SpanUtils;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import net.bytebuddy.implementation.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import zipkin2.Endpoint;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.Map;

import static brave.Span.Kind.CONSUMER;

/**
 * @author hzfjd
 * @create 18/11/23
 **/
public class MessageListenerContainerTraceInterceptor {

    private static Logger                     logger                            = LoggerFactory.getLogger(MessageListenerContainerTraceInterceptor.class);

    private static final ThreadLocal<Boolean> invokeListerInterceptedMarkHolder = new ThreadLocal();

    @RuntimeType
    public static Object intercept(@AllArguments Object[] args, @Morph Invoker invoker, @Origin Method method,
                                   @This Object proxy) throws Exception {

        // 为了兼容1和2版本的spring
        // rabbit，所以在AbstractMessageListenerContainer和SimpleMessageListenerContainer都做了相同的拦截。因此需要这里这下防重保护。
        if (Boolean.TRUE.equals(invokeListerInterceptedMarkHolder.get())) {
            return invoker.invoke(args);
        }

        try {
            invokeListerInterceptedMarkHolder.set(true);
            return innerTraceIntercept(args, invoker, method, proxy);
        } finally {
            invokeListerInterceptedMarkHolder.remove();
        }

    }

    private static Object innerTraceIntercept(Object[] args, Invoker invoker, Method method,
                                              Object proxy) throws Exception {

        // do for "invokeListener" method
        RabbitTracing rabbitTracing = SpringBeanFactorySupport.getBean(RabbitTracing.class);
        if (rabbitTracing == null) {
            return invoker.invoke(args);
        }

        Channel channel = (Channel) args[0];
        Message message = (Message) args[1];

        TraceContext.Extractor<MessageProperties> extractor = rabbitTracing.tracing().propagation().extractor(GETTER);
        TraceContextOrSamplingFlags extracted = extractTraceContextAndRemoveHeaders(message, rabbitTracing, extractor);

        Tracer tracer = rabbitTracing.tracing().tracer();

        // 无上游追踪信息的消费者，一般为一些死循环任务。不做追踪支持。
        if (extracted == null || extracted.context() == null) {
            return invoker.invoke(args);
        }

        Span consumerSpan = tracer.nextSpan(extracted).kind(CONSUMER).name("on-message");
        SpanUtils.safeTag(consumerSpan, SpanType.TAG_KEY, SpanType.RABBIT);
        SpanUtils.safeTag(consumerSpan, CommonTagKeys.SERVER_ENV, EnvironmentUtils.getCurrentEnv());

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

    static TraceContextOrSamplingFlags extractTraceContextAndRemoveHeaders(Message message, RabbitTracing rabbitTracing,
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
