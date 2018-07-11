package com.netease.edu.eds.trace.instrument.rabbit;

import brave.Span;
import brave.Tracer;
import brave.propagation.Propagation;
import brave.propagation.TraceContext;
import com.netease.edu.eds.trace.core.Invoker;
import com.netease.edu.eds.trace.spi.TraceAgentInstrumetation;
import com.netease.edu.eds.trace.support.DefaultAgentBuilderListener;
import com.netease.edu.eds.trace.support.SpringBeanFactorySupport;
import com.rabbitmq.client.Channel;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.Morph;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.support.CorrelationData;
import zipkin2.Endpoint;

import java.lang.instrument.Instrumentation;
import java.util.Map;
import java.util.concurrent.Callable;

import static net.bytebuddy.matcher.ElementMatchers.isOverriddenFrom;
import static net.bytebuddy.matcher.ElementMatchers.namedIgnoreCase;

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
                                                                                                                           javaModule) -> builder.method(isOverriddenFrom(typeDescription).and(namedIgnoreCase("doSend"))).intercept(MethodDelegation.to(TraceInterceptor.class))).with(DefaultAgentBuilderListener.getInstance()).installOn(inst);

    }

    public static class TraceInterceptor {

        public static void doSend(@Argument(0) Channel channel, @Argument(1) String exchange,
                                  @Argument(2) String routingKey, @Argument(3) Message message,
                                  @Argument(4) boolean mandatory, @Argument(5) CorrelationData correlationData,
                                  @SuperCall Callable<Void> originalCall, @AllArguments Object[] args,
                                  @Morph Invoker invoker) throws Exception {

            RabbitTracing rabbitTracing = SpringBeanFactorySupport.getBean(RabbitTracing.class);
            if (rabbitTracing == null) {
                originalCall.call();
                return;
            }

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
                invoker.invoke(args);
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
