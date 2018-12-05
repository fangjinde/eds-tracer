package com.netease.edu.eds.trace.instrument.rabbit;

import brave.Span;
import brave.Tracer;
import brave.propagation.Propagation;
import brave.propagation.TraceContext;
import com.netease.edu.eds.trace.constants.CommonTagKeys;
import com.netease.edu.eds.trace.constants.SpanType;
import com.netease.edu.eds.trace.core.Invoker;
import com.netease.edu.eds.trace.spi.TraceAgentInstrumetation;
import com.netease.edu.eds.trace.support.AgentSupport;
import com.netease.edu.eds.trace.support.DefaultAgentBuilderListener;
import com.netease.edu.eds.trace.support.SpringBeanFactorySupport;
import com.netease.edu.eds.trace.utils.EnvironmentUtils;
import com.netease.edu.eds.trace.utils.SpanUtils;
import com.rabbitmq.client.Channel;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Morph;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import zipkin2.Endpoint;

import java.lang.instrument.Instrumentation;
import java.util.Map;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * @author hzfjd
 * @create 18/7/10
 **/
public class RabbitTemplateInstrumentation implements TraceAgentInstrumetation {

    @Override
    public void premain(Map<String, String> props, Instrumentation inst) {

        new AgentBuilder.Default().type(namedIgnoreCase("org.springframework.amqp.rabbit.core.RabbitTemplate")).transform((builder,
                                                                                                                           typeDescription,
                                                                                                                           classloader,
                                                                                                                           javaModule) -> builder.method(isOverriddenFrom(typeDescription).and(namedIgnoreCase("doSend")).and(takesArguments(6))).intercept(AgentSupport.getInvokerMethodDelegationCustomer().to(TraceInterceptor.class))).with(DefaultAgentBuilderListener.getInstance()).installOn(inst);

    }

    public static class TraceInterceptor {

        private static Logger logger = LoggerFactory.getLogger(TraceInterceptor.class);

        @RuntimeType
        public static Object doSend(@AllArguments Object[] args, @Morph Invoker invoker) throws Exception {

            return traceDoSend(args, invoker);
        }

        private static Object traceDoSend(Object[] args, Invoker invoker) throws Exception {

            RabbitTracing rabbitTracing = SpringBeanFactorySupport.getBean(RabbitTracing.class);
            if (rabbitTracing == null) {
                return invoker.invoke(args);
            }

            Channel channel = (Channel) args[0];
            String exchange = (String) args[1];
            String routingKey = (String) args[2];
            Message message = (Message) args[3];

            TraceContext.Injector<MessageProperties> injector = rabbitTracing.tracing().propagation().injector(SETTER);

            Tracer tracer = rabbitTracing.tracing().tracer();
            Span span = tracer.nextSpan().kind(Span.Kind.PRODUCER).name("publish");

            try (Tracer.SpanInScope spanInScope = tracer.withSpanInScope(span)) {

                if (!span.isNoop()) {

                    SpanUtils.safeTag(span, SpanType.TAG_KEY, SpanType.RABBIT);
                    injector.inject(span.context(), message.getMessageProperties());

                    RabbitTracing.tagSendMessageInfo(span, exchange, routingKey);
                    RabbitTracing.tagMessagePayload(span, message.toString());
                    SpanUtils.safeTag(span, CommonTagKeys.CLIENT_ENV, EnvironmentUtils.getCurrentEnv());
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
                SpanUtils.tagPropagationInfos(span);
                return invoker.invoke(args);
            } catch (Exception e) {
                SpanUtils.tagErrorMark(span);
                SpanUtils.tagError(span, e);
                throw e;
            } finally {
                span.finish();
            }

        }

        static final Propagation.Setter<MessageProperties, String> SETTER = new Propagation.Setter<MessageProperties, String>() {

            @Override
            public void put(MessageProperties carrier, String key, String value) {
                carrier.setHeader(key, value);
            }

            @Override
            public String toString() {
                return "MessageProperties::setHeader";
            }
        };
    }
}
