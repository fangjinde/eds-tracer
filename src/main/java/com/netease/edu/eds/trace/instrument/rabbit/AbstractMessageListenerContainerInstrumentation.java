package com.netease.edu.eds.trace.instrument.rabbit;

import brave.Span;
import brave.Tracer;
import brave.propagation.Propagation;
import brave.propagation.TraceContext;
import brave.propagation.TraceContextOrSamplingFlags;
import com.netease.edu.eds.shuffle.core.BeanNameConstants;
import com.netease.edu.eds.shuffle.core.EnvironmentShuffleUtils;
import com.netease.edu.eds.shuffle.core.ShufflePropertiesSupport;
import com.netease.edu.eds.shuffle.core.ShuffleSwitch;
import com.netease.edu.eds.shuffle.spi.EnvironmentDetector;
import com.netease.edu.eds.shuffle.spi.KeyValueManager;
import com.netease.edu.eds.shuffle.support.InterProcessMutexContext;
import com.netease.edu.eds.trace.constants.SpanType;
import com.netease.edu.eds.trace.core.Invoker;
import com.netease.edu.eds.trace.spi.TraceAgentInstrumetation;
import com.netease.edu.eds.trace.support.AgentSupport;
import com.netease.edu.eds.trace.support.DefaultAgentBuilderListener;
import com.netease.edu.eds.trace.support.SpringBeanFactorySupport;
import com.netease.edu.eds.trace.utils.MD5Utils;
import com.netease.edu.eds.trace.utils.PropagationUtils;
import com.netease.edu.eds.trace.utils.SpanUtils;
import com.rabbitmq.client.Channel;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Morph;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import zipkin2.Endpoint;

import java.lang.instrument.Instrumentation;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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

        /**
         * case1 当前环境接收处理; case2 当前环境delay处理; case3 当前环境丢弃处理;
         * 
         * @param stdEnv
         * @param originAndStdEnvs
         * @param currentApplicationName
         * @return
         */
        private static int getCaseForStdConsumer(String stdEnv, List<String> originAndStdEnvs,
                                                 String currentApplicationName) {
            // 请求来源是std
            String originEnv = PropagationUtils.getOriginEnv();
            if (StringUtils.isBlank(originEnv) || stdEnv.equals(originEnv)) {
                return 1;
            }

            if (CollectionUtils.isEmpty(originAndStdEnvs) || originAndStdEnvs.size() != 2
                || !originAndStdEnvs.contains(stdEnv)) {
                return 1;
            }

            // 请求来源为测试环境
            String testEnv = null;
            Iterator<String> envIterator = originAndStdEnvs.iterator();
            while (envIterator.hasNext()) {
                String nextEnv = envIterator.next();
                if (!stdEnv.equals(nextEnv)) {
                    testEnv = nextEnv;
                }
            }

            // case2 当前环境delay处理

            // case3 当前环境丢弃处理
            return 3;
        }

        public static Object shuffleInvokeListener(Object[] args, Invoker invoker) throws Exception {

            if (!ShuffleSwitch.isTurnOn()) {
                return invoker.invoke(args);
            }
            Channel channel = (Channel) args[0];
            Message message = (Message) args[1];

            String currentApplicationName = EnvironmentShuffleUtils.getCurrentApplicationName();
            String curEnv = EnvironmentShuffleUtils.getCurrentEnv();
            String stdEnv = ShufflePropertiesSupport.getStandardEnvName();
            List<String> originAndStdEnvs = EnvironmentShuffleUtils.getOriginAndStdEnvironments();

            EnvironmentDetector environmentDetector = SpringBeanFactorySupport.getBean(EnvironmentDetector.class);

            InterProcessMutexContext queueConsumerMutexContext = SpringBeanFactorySupport.getBean(BeanNameConstants.QUEUE_CONSUMER_MUTEX_CONTEXT);

            KeyValueManager keyValueManager = SpringBeanFactorySupport.getBean(KeyValueManager.class);

            String messageKey = getMessageIdKey(message);
            String messageOwnerEnvKey = messageKey + "-OwnerEnv";
            String messageStdInQueueCountKey = messageKey + "-" + stdEnv + "-InQueueCount";

            if (stdEnv.equals(curEnv)) {
                // 当前为基准环境
                // 检查现有环境占用情况
                String ownerEnv = keyValueManager.getValue(messageOwnerEnvKey);

                // 其他环境已经消费
                if (StringUtils.isNotBlank(ownerEnv) && !ownerEnv.equals(curEnv)) {
                    // 过滤该消息。如果发现对于rabbit事务消息的场景有影响的话，再做进一步的细化处理。例如及时提交事务，关闭channel等。
                    return null;
                }

                return invoker.invoke(args);
            } else {
                // 当前为测试环境

                // 检查现有环境占用情况
                String ownerEnv = keyValueManager.getValue(messageOwnerEnvKey);

                // 其他环境已经消费
                if (StringUtils.isNotBlank(ownerEnv) && !ownerEnv.equals(curEnv)) {
                    // 过滤该消息。如果发现对于rabbit事务消息的场景有影响的话，再做进一步的细化处理。例如及时提交事务，关闭channel等。
                    return null;
                }

                InterProcessMutex lock = queueConsumerMutexContext.getLock(messageOwnerEnvKey);
                boolean acquired = lock.acquire(3600, TimeUnit.SECONDS);
                // 长时间仍锁定超时，丢弃该消息。即由能获取锁定的消费者处理(基准环境）
                if (!acquired) {
                    return null;
                }
                // 获取锁后
                try {
                    // 二次检查环境是否占有
                    ownerEnv = keyValueManager.getValue(messageOwnerEnvKey);
                    // 其他环境已经消费
                    if (StringUtils.isNotBlank(ownerEnv) && !ownerEnv.equals(curEnv)) {
                        // 过滤该消息。如果发现对于rabbit事务消息的场景有影响的话，再做进一步的细化处理。例如及时提交事务，关闭channel等。
                        return null;
                    }
                    // 执行消息消费
                    try {
                        Object result = invoker.invoke(args);
                    } finally {
                        // 不管消费过程有没有异常，都需要更新环境占用标记
                        if (!curEnv.equals(ownerEnv)) {
                            keyValueManager.setValue(messageOwnerEnvKey, curEnv);
                        }
                    }

                } finally {
                    // 释放锁
                    lock.release();

                }

            }

            return invoker.invoke(args);
        }

        private static String getMessageIdKey(Message message) {
            return MD5Utils.digest(message.getBody());
        }

        @RuntimeType
        public static Object invokeListener(@AllArguments Object[] args, @Morph Invoker invoker) throws Exception {

            RabbitTracing rabbitTracing = SpringBeanFactorySupport.getBean(RabbitTracing.class);
            if (rabbitTracing == null) {
                return shuffleInvokeListener(args, invoker);
            }

            Channel channel = (Channel) args[0];
            Message message = (Message) args[1];

            TraceContext.Extractor<MessageProperties> extractor = rabbitTracing.tracing().propagation().extractor(GETTER);
            TraceContextOrSamplingFlags extracted = extractTraceContextAndRemoveHeaders(message, rabbitTracing,
                                                                                        extractor);

            Tracer tracer = rabbitTracing.tracing().tracer();
            Span consumerSpan = tracer.nextSpan(extracted).kind(CONSUMER).name("on-message");
            SpanUtils.safeTag(consumerSpan, SpanType.TAG_KEY, SpanType.RABBIT);

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
                return shuffleInvokeListener(args, invoker);
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
