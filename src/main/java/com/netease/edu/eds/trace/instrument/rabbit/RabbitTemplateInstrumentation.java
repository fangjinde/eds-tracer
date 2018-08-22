package com.netease.edu.eds.trace.instrument.rabbit;

import brave.Span;
import brave.Tracer;
import brave.propagation.Propagation;
import brave.propagation.TraceContext;
import com.netease.edu.eds.shuffle.core.EnvironmentShuffleUtils;
import com.netease.edu.eds.shuffle.core.ShufflePropertiesSupport;
import com.netease.edu.eds.shuffle.core.ShuffleSwitch;
import com.netease.edu.eds.trace.constants.SpanType;
import com.netease.edu.eds.trace.core.Invoker;
import com.netease.edu.eds.trace.spi.TraceAgentInstrumetation;
import com.netease.edu.eds.trace.support.AgentSupport;
import com.netease.edu.eds.trace.support.DefaultAgentBuilderListener;
import com.netease.edu.eds.trace.support.SpringBeanFactorySupport;
import com.netease.edu.eds.trace.utils.SpanUtils;
import com.rabbitmq.client.Channel;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Morph;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import zipkin2.Endpoint;

import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

            if (!ShuffleSwitch.isTurnOn()) {
                return traceDoSend(args, invoker);

            }

            String exchange = (String) args[1];
            List<String> envsForSelection = EnvironmentShuffleUtils.getEnvironmentsForPropagationSelection();
            List<String> allShuffleExchanges = getAllShuffleExchangeToSend(envsForSelection, exchange);
            if (CollectionUtils.isEmpty(allShuffleExchanges)) {
                return traceDoSend(args, invoker);
            }

            Object originResult = null;

            int loopIndex = 0;
            for (String shuffleExchange : allShuffleExchanges) {
                args[1] = shuffleExchange;
                if (loopIndex >= 1) {
                    doDelayForNextSend();
                }
                Object result = traceDoSend(args, invoker);
                if (exchange.equals(shuffleExchange)) {
                    originResult = result;
                }
                loopIndex++;
            }

            return originResult;

        }

        /**
         * wait 150ms to send message to latter environment to reduce cross environment consumer compete as less as
         * possible.
         * 
         * @throws InterruptedException
         */
        private static void doDelayForNextSend() throws InterruptedException {
            int delayMs = ShufflePropertiesSupport.getDelayMSToSendLatter();
            if (delayMs > 0) {
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    logger.error("doDelayForNextSend try to sleep, but got interrupted.", e);
                    throw e;
                }
            }
        }

        private static List<String> getAllShuffleExchangeToSend(List<String> envsForSelection, String exchange) {

            if (CollectionUtils.isEmpty(envsForSelection)) {
                return null;
            }

            String originExchangeEnv = null;
            for (String env : envsForSelection) {
                if (exchange.startsWith(env) || exchange.endsWith(env)) {
                    originExchangeEnv = env;
                    break;
                }
            }
            // 为规避cloud bus等统一环境exchange的情况，原exchange若无环境属性，则不做shuffle处理
            if (originExchangeEnv == null) {
                return null;
            }

            List<String> allShuffleExchanges = new ArrayList<>();
            allShuffleExchanges.add(exchange);
            // 增加其他环境的exchange，修改对应的环境属性
            for (String newEnv : envsForSelection) {

                if (!originExchangeEnv.equals(newEnv)) {
                    String newShuffleExchange = getNewExchangeWithNewEnv(exchange, originExchangeEnv, newEnv);
                    if (StringUtils.isNotBlank(newShuffleExchange)) {
                        allShuffleExchanges.add(newShuffleExchange);
                    }
                }
            }

            return allShuffleExchanges;

        }

        public static void main(String[] args) {

            String exchange1 = "memberRegisterExchange-p1";
            String exchange2 = "std-memberRegisterExchange";
            String exchange3 = "memberRegisterExchange";

            System.out.println(getNewExchangeWithNewEnv(exchange1, "p1", "std"));
            System.out.println(getNewExchangeWithNewEnv(exchange2, "std", "p1"));
            System.out.println(getNewExchangeWithNewEnv(exchange3, "p1", "std"));

            System.out.println(getAllShuffleExchangeToSend(Arrays.asList("p1", "std"), exchange1));
            System.out.println(getAllShuffleExchangeToSend(Arrays.asList("p1", "std"), exchange2));
            System.out.println(getAllShuffleExchangeToSend(Arrays.asList("p1", "std"), exchange3));

            System.out.println(getAllShuffleExchangeToSend(Arrays.asList("std"), exchange1));
            System.out.println(getAllShuffleExchangeToSend(Arrays.asList("std"), exchange2));
            System.out.println(getAllShuffleExchangeToSend(Arrays.asList("std"), exchange3));

        }

        private static String getNewExchangeWithNewEnv(String exchange, String originEnv, String newEnv) {
            StringBuilder newExchangeSb = new StringBuilder();
            if (exchange.startsWith(originEnv)) {
                newExchangeSb.append(newEnv).append(exchange.substring(originEnv.length()));
            } else if (exchange.endsWith(originEnv)) {
                newExchangeSb.append(exchange.substring(0, exchange.length() - originEnv.length())).append(newEnv);
            }
            return newExchangeSb.toString();
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
