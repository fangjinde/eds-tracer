package com.netease.edu.eds.trace.instrument.rabbit;

import brave.Span;
import brave.Tracer;
import brave.propagation.Propagation;
import brave.propagation.TraceContext;
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

        @RuntimeType
        public static Object doSend(@AllArguments Object[] args, @Morph Invoker invoker) throws Exception {

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
                return invoker.invoke(args);
            } catch (Exception e) {
                RabbitTracing.tagErrorSpan(span, e);
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
