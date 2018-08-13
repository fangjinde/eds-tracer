package com.netease.edu.eds.trace.instrument.rabbit;

import brave.Span;
import brave.Tracer;
import brave.Tracing;
import brave.propagation.Propagation;
import brave.propagation.TraceContext;
import brave.propagation.TraceContextOrSamplingFlags;
import com.netease.edu.eds.shuffle.core.*;
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
import com.rabbitmq.client.Connection;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bind.annotation.*;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.ChannelAwareMessageListener;
import org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer;
import org.springframework.util.ReflectionUtils;
import zipkin2.Endpoint;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static brave.Span.Kind.CONSUMER;
import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * @author hzfjd
 * @create 18/7/10
 **/
public class AbstractMessageListenerContainerInstrumentation implements TraceAgentInstrumetation {

    private ElementMatcher.Junction getMatcher(TypeDescription typeDescription) {
        ElementMatcher.Junction matcher1 = isDeclaredBy(typeDescription).and(namedIgnoreCase("invokeListener")).and(takesArguments(2));
        ElementMatcher.Junction matcher2 = isDeclaredBy(typeDescription).and(namedIgnoreCase("getMessageListener")).and(takesArguments(0));
        return matcher1.or(matcher2);
    }

    @Override
    public void premain(Map<String, String> props, Instrumentation inst) {

        new AgentBuilder.Default().type(namedIgnoreCase("org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer")).transform((builder,
                                                                                                                                                 typeDescription,
                                                                                                                                                 classloader,
                                                                                                                                                 javaModule) -> builder.method(getMatcher(typeDescription)).intercept(AgentSupport.getInvokerMethodDelegationCustomer().to(Interceptor.class))).with(DefaultAgentBuilderListener.getInstance()).installOn(inst);

    }

    public static class Interceptor {

        private static AtomicReference<Method> actualInvokeListenerMethod = new AtomicReference<>();

        private static Logger                  logger                     = LoggerFactory.getLogger(Interceptor.class);

        private static ThreadLocal<Integer>    processModeThreadLocal     = new ThreadLocal<>();

        static int                             PROCESS                    = 1;
        static int                             DELAY                      = 2;
        static int                             IGNORE                     = 3;

        /**
         * case1 当前环境接收处理; case2 当前环境delay处理; case3 当前环境丢弃处理;
         * 
         * @param stdEnv
         * @param originAndStdEnvs
         * @param currentApplicationName
         * @return
         */
        private static int getCaseForStdConsumer(String stdEnv, List<String> originAndStdEnvs,
                                                 String currentApplicationName, KeyValueManager keyValueManager,
                                                 String messageOwnerEnvKey,
                                                 String messageDelayAndRouteBackStdQueueCountKey,
                                                 EnvironmentDetector environmentDetector) {

            // 请求来源是std
            String originEnv = PropagationUtils.getOriginEnv();
            if (StringUtils.isBlank(originEnv) || stdEnv.equals(originEnv)) {
                return PROCESS;
            }

            if (CollectionUtils.isEmpty(originAndStdEnvs) || originAndStdEnvs.size() != 2
                || !originAndStdEnvs.contains(stdEnv)) {
                return PROCESS;
            }

            // 以下仅需处理请求来源为测试环境的情况
            String testEnv = null;
            Iterator<String> envIterator = originAndStdEnvs.iterator();
            while (envIterator.hasNext()) {
                String nextEnv = envIterator.next();
                if (!stdEnv.equals(nextEnv)) {
                    testEnv = nextEnv;
                }
            }

            // 检查现有环境占用情况
            String ownerEnv = keyValueManager.getValue(messageOwnerEnvKey);
            if (stdEnv.equals(ownerEnv)) {
                return PROCESS;
            }

            if (StringUtils.isNotBlank(ownerEnv) && !testEnv.equals(ownerEnv)) {
                // 别的乱七八糟的环境，则交由std兼容处理。
                return PROCESS;
            }

            int STD_IN_QUEUE_COUNT_THRESHOLD = 3;
            boolean testEnvExisted = environmentDetector.exist(testEnv, currentApplicationName);
            // 测试环境占用标记存在
            if (testEnv.equals(ownerEnv)) {

                if (testEnvExisted) {
                    // 如果测试环境存在，并且有消费过的标记。则std当前消息可以丢弃。如果后续测试环境重试产生消息，但环境被后续销毁，消息也会通过死信队列重新转移到std队列。因此当前消息是可以丢弃的。
                    return IGNORE;
                }
                Long messageStdInQueueCount = keyValueManager.increment(messageDelayAndRouteBackStdQueueCountKey, 0L,
                                                                        ShuffleConstants.DUPLICATE_CHECK_VALID_PERIOD);
                // 测试环境不存活。需要区分测试环境是暂时下线，还是销毁。通过重入队列次数，亦即MESSAGE TTL周期数。
                if (messageStdInQueueCount > STD_IN_QUEUE_COUNT_THRESHOLD) {
                    // 销毁
                    return PROCESS;
                } else {
                    // 暂时下线,delay处理。
                    return DELAY;
                }

            }

            // 以下仅需处理来源为测试环境，且未有环境占用标记的情况。
            if (testEnvExisted) {

                return DELAY;
            }

            return PROCESS;
        }

        private static Object byPassProxyIfPossible(Object[] args, Invoker invoker, Object proxy) {
            // protected void actualInvokeListener(Channel channel, Message message) throws Exception
            Method method = actualInvokeListenerMethod.get();
            if (method == null) {
                method = ReflectionUtils.findMethod(AbstractMessageListenerContainer.class, "actualInvokeListener",
                                                    Channel.class, Message.class);
                if (method != null) {
                    ReflectionUtils.makeAccessible(method);
                }
                actualInvokeListenerMethod.set(method);
            }

            if (method == null) {
                logger.error("miss AbstractMessageListenerContainer:actualInvokeListener method, using origin method. which might cause unknown errors.");
                return invoker.invoke(args);
            }

            try {
                return method.invoke(proxy, args);
            } catch (IllegalAccessException e) {
                logger.error("IllegalAccessException while calling AbstractMessageListenerContainer:actualInvokeListener method, using origin method. which might cause unknown errors.");
                return invoker.invoke(args);
            } catch (InvocationTargetException e) {
                // 需要确保抛出异常为spring rabbit设计范围内。否则可能会导致连接异常。后续可以深入研究下。
                if (e.getCause() instanceof AmqpException) {
                    throw (AmqpException) e.getCause();
                } else {
                    throw new AmqpException("InvocationTargetException while  calling to AbstractMessageListenerContainer:actualInvokeListener error.",
                                            e);
                }

            }
        }

        private static Object doIgnore(Object[] args, Invoker invoker, Object proxy) {
            // 标记IGNORE，后续messageListen会被替换为IGNORE实现。
            processModeThreadLocal.set(IGNORE);
            try {
                return byPassProxyIfPossible(args, invoker, proxy);

            } finally {
                processModeThreadLocal.remove();
            }
        }

        private static Object doDelay(Object[] args, Invoker invoker, KeyValueManager keyValueManager,
                                      String messageDelayAndRouteBackStdQueueCountKey, Object proxy) {
            // 标记delay，后续messageListen会被替换为delay实现。
            processModeThreadLocal.set(DELAY);
            try {
                return byPassProxyIfPossible(args, invoker, proxy);
            } finally {
                processModeThreadLocal.remove();
                // 累加delay次数
                keyValueManager.increment(messageDelayAndRouteBackStdQueueCountKey, 1,
                                          ShuffleConstants.DUPLICATE_CHECK_VALID_PERIOD);

            }

        }

        private static Object doRealProcess(Object[] args, Invoker invoker, KeyValueManager keyValueManager,
                                            String messageOwnerEnvKey, String curEnv) {
            // 执行消息消费
            try {
                return invoker.invoke(args);
            } finally {
                // 不管消费过程有没有异常，都需要更新环境占用标记
                String ownerEnv = keyValueManager.getValue(messageOwnerEnvKey);
                if (!curEnv.equals(ownerEnv)) {
                    keyValueManager.setValue(messageOwnerEnvKey, curEnv, ShuffleConstants.DUPLICATE_CHECK_VALID_PERIOD);
                }
            }
        }

        private static Object processForStd(String stdEnv, List<String> originAndStdEnvs, String currentApplicationName,
                                            KeyValueManager keyValueManager, String messageOwnerEnvKey,
                                            String messageDelayAndRouteBackStdQueueCountKey,
                                            EnvironmentDetector environmentDetector,
                                            InterProcessMutexContext queueConsumerMutexContext, String curEnv,
                                            String messageKey, Object[] args, Invoker invoker, Object proxy) {
            // 当前为基准环境
            int caseToProcess = getCaseForStdConsumer(stdEnv, originAndStdEnvs, currentApplicationName, keyValueManager,
                                                      messageOwnerEnvKey, messageDelayAndRouteBackStdQueueCountKey,
                                                      environmentDetector);
            if (DELAY == caseToProcess) {

                return doDelay(args, invoker, keyValueManager, messageDelayAndRouteBackStdQueueCountKey, proxy);

            } else if (PROCESS == caseToProcess) {

                InterProcessMutex lock = queueConsumerMutexContext.getLock(messageOwnerEnvKey);
                boolean acquired = false;
                try {
                    acquired = lock.acquire(3600, TimeUnit.SECONDS);
                } catch (Exception e) {
                    return doDelay(args, invoker, keyValueManager, messageDelayAndRouteBackStdQueueCountKey, proxy);
                }
                // 长时间仍锁定超时，丢弃该消息。即由能获取锁定的消费者处理(基准环境）
                if (!acquired) {
                    return doIgnore(args, invoker, proxy);
                }
                // 获取锁后
                try {

                    // 二次检查
                    int caseToProcessSecondTimeCheck = getCaseForStdConsumer(stdEnv, originAndStdEnvs,
                                                                             currentApplicationName, keyValueManager,
                                                                             messageOwnerEnvKey,
                                                                             messageDelayAndRouteBackStdQueueCountKey,
                                                                             environmentDetector);
                    if (PROCESS == caseToProcessSecondTimeCheck) {
                        // 执行消息消费
                        return doRealProcess(args, invoker, keyValueManager, messageOwnerEnvKey, curEnv);
                    } else if (DELAY == caseToProcessSecondTimeCheck) {
                        return doDelay(args, invoker, keyValueManager, messageDelayAndRouteBackStdQueueCountKey, proxy);
                    } else {
                        return doIgnore(args, invoker, proxy);
                    }

                } finally {
                    // 释放锁
                    try {
                        lock.release();
                    } catch (Exception e) {
                        logger.error("release zookeeper lock error, ", e);
                        // 上面步骤已经完成对应执行执行,此处不要调用 doIgnore()
                    }

                }
            } else {
                return doIgnore(args, invoker, proxy);
            }
        }

        public static Object shuffleInvokeListener(Object[] args, Invoker invoker, Object proxy) throws Exception {

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
            String messageDelayAndRouteBackStdQueueCountKey = messageKey + "-" + stdEnv + "-RouteBackCount";

            if (stdEnv.equals(curEnv)) {
                // 当前为基准环境
                return processForStd(stdEnv, originAndStdEnvs, currentApplicationName, keyValueManager,
                                     messageOwnerEnvKey, messageDelayAndRouteBackStdQueueCountKey, environmentDetector,
                                     queueConsumerMutexContext, curEnv, messageKey, args, invoker, proxy);

            }
            // 当前为测试环境

            // 检查现有环境占用情况
            String ownerEnv = keyValueManager.getValue(messageOwnerEnvKey);

            // 其他环境已经消费
            if (StringUtils.isNotBlank(ownerEnv) && !ownerEnv.equals(curEnv)) {
                // 过滤该消息。如果发现对于rabbit事务消息的场景有影响的话，再做进一步的细化处理。例如及时提交事务，关闭channel等。
                return doIgnore(args, invoker, proxy);
            }

            InterProcessMutex lock = queueConsumerMutexContext.getLock(messageOwnerEnvKey);
            boolean acquired = lock.acquire(3600, TimeUnit.SECONDS);
            // 长时间仍锁定超时，丢弃该消息。即由能获取锁定的消费者处理(基准环境）
            if (!acquired) {
                return doIgnore(args, invoker, proxy);
            }
            // 获取锁后
            try {
                // 二次检查环境是否占有
                ownerEnv = keyValueManager.getValue(messageOwnerEnvKey);
                // 其他环境已经消费
                if (StringUtils.isNotBlank(ownerEnv) && !ownerEnv.equals(curEnv)) {
                    // 过滤该消息。如果发现对于rabbit事务消息的场景有影响的话，再做进一步的细化处理。例如及时提交事务，关闭channel等。
                    return doIgnore(args, invoker, proxy);
                }
                // 执行消息消费
                return doRealProcess(args, invoker, keyValueManager, messageOwnerEnvKey, curEnv);

            } finally {
                // 释放锁
                lock.release();

            }

        }

        private static String getMessageIdKey(Message message) {
            try {
                return Tracing.currentTracer().currentSpan().context().traceIdString();
            } catch (Exception e) {
                return MD5Utils.digest(message.getBody());
            }

        }

        public static Object getMessageListener(Object[] args, Invoker invoker) throws Exception {

            Integer processMode = processModeThreadLocal.get();

            Object retObject = invoker.invoke(args);

            if (new Integer(DELAY).equals(processMode)) {
                String errorMessage = "reject caused by current shuffled consumer can't decide yet, wait next ttl comes to decide.";
                if (retObject instanceof MessageListener) {
                    return new MessageListener() {

                        @Override
                        public void onMessage(Message message) {
                            throw new AmqpRejectAndDontRequeueException(errorMessage);
                        }
                    };

                } else if (retObject instanceof ChannelAwareMessageListener) {
                    return new ChannelAwareMessageListener() {

                        @Override
                        public void onMessage(Message message, Channel channel) throws Exception {
                            throw new AmqpRejectAndDontRequeueException(errorMessage);
                        }
                    };
                }

            } else if (new Integer(IGNORE).equals(processMode)) {
                if (retObject instanceof MessageListener) {
                    return new MessageListener() {

                        @Override
                        public void onMessage(Message message) {
                        }
                    };

                } else if (retObject instanceof ChannelAwareMessageListener) {
                    return new ChannelAwareMessageListener() {

                        @Override
                        public void onMessage(Message message, Channel channel) throws Exception {
                        }
                    };
                }
            }
            return retObject;

        }

        @RuntimeType
        public static Object intercept(@AllArguments Object[] args, @Morph Invoker invoker, @Origin Method method,
                                       @This Object proxy) throws Exception {

            // do for "getMessageListener" method
            if (method.getName().equals("getMessageListener")) {
                return getMessageListener(args, invoker);
            }

            // do for "invokeListener" method
            RabbitTracing rabbitTracing = SpringBeanFactorySupport.getBean(RabbitTracing.class);
            if (rabbitTracing == null) {
                return invoker.invoke(args);
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
                return shuffleInvokeListener(args, invoker, proxy);
            } catch (Throwable t) {
                SpanUtils.tagErrorMark(consumerSpan);
                SpanUtils.tagError(consumerSpan, t);
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
