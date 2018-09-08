package com.netease.edu.eds.trace.instrument.rabbit;

import brave.Span;
import brave.Tracer;
import brave.Tracing;
import brave.propagation.Propagation;
import brave.propagation.TraceContext;
import brave.propagation.TraceContextOrSamplingFlags;
import com.netease.edu.eds.shuffle.core.*;
import com.netease.edu.eds.shuffle.instrument.rabbit.RabbitShuffleSendContext;
import com.netease.edu.eds.shuffle.spi.EnvironmentDetector;
import com.netease.edu.eds.shuffle.spi.KeyValueManager;
import com.netease.edu.eds.shuffle.support.InterProcessMutexContext;
import com.netease.edu.eds.shuffle.support.ShuffleEnvironmentInfoProcessUtils;
import com.netease.edu.eds.trace.constants.SpanType;
import com.netease.edu.eds.trace.core.Invoker;
import com.netease.edu.eds.trace.spi.TraceAgentInstrumetation;
import com.netease.edu.eds.trace.support.AgentSupport;
import com.netease.edu.eds.trace.support.DefaultAgentBuilderListener;
import com.netease.edu.eds.trace.support.SpringBeanFactorySupport;
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
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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
import java.util.UUID;
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
        private static int getCaseForStdConsumer(Message message, String stdEnv, List<String> originAndStdEnvs,
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

                // 测试环境标记过，目前当前消息为对应回游消息，则std消费之。
                if (ShuffleConstants.SHUFFLE_ROUTE_BACK_EXCHANGE.equals(message.getMessageProperties().getReceivedExchange())) {
                    return PROCESS;
                }

                // 即便此时测试环境不存在，也仍等待测试环境处理。直到消息过期，转回游消息。
                // 测试环境标记过,当前消息为非回游消息，std忽略之。回游消息总会来的。
                return IGNORE;

            }

            // 以下仅需处理来源为测试环境，且未有环境占用标记的情况。
            if (!testEnvExisted) {
                // 如果环境未被测试环境声明，且测试环境不存活，则直接有std消费。
                // 在发送端，对std的消息发送，会默认延迟100ms发送，以保证大多数情况下，std消息的声明是后于测试环境的。
                // 受服务发现延迟的影响，这里的确会有一定的误判。但并不会导致声明严重的后果。因为事实上，测试环境真正不存在的情况也是允许的。
                return PROCESS;
            }

            // 测试环境未被暂用过，且当前测试环境存活着，主要如下几种可能：

            // 1. 测试环境消息消费存在堆积，std等待N个重试周期。
            // 2. 发送消息时，消费者的测试环境未创建，即其没有接受到该消息（这种情况比较极端的情况。因为队列是持久的。 但仍会重试多次后得到std处理。）
            Long messageStdInQueueCount = keyValueManager.increment(messageDelayAndRouteBackStdQueueCountKey, 0L,
                                                                    ShuffleConstants.DUPLICATE_CHECK_VALID_PERIOD);
            // 测试环境不存活。需要区分测试环境是暂时下线，还是销毁。通过重入队列次数，亦即MESSAGE TTL周期数。
            // 延迟3小时后再由原业务重试。正常情况下，走到延迟队列的都不需要及时处理。重试次数4*重试间隔3h=12小时后，测试环境暂用过，却又不存活的，则std接管处理。
            if (messageStdInQueueCount > STD_IN_QUEUE_COUNT_THRESHOLD) {
                // 销毁
                return PROCESS;
            }
            // 暂时下线,delay处理。
            return DELAY;

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
                                      String messageDelayAndRouteBackStdQueueCountKey, Object proxy, Message message) {
            // 标记delay，后续messageListen会被替换为delay实现。
            processModeThreadLocal.set(DELAY);
            RabbitShuffleSendContext.ignoreShuffle();
            RabbitShuffleSendContext.getDelaySendContextThreadLocal().set(new RabbitShuffleSendContext.DelaySendContext(message));
            try {
                return byPassProxyIfPossible(args, invoker, proxy);
            } finally {
                RabbitShuffleSendContext.getDelaySendContextThreadLocal().remove();
                RabbitShuffleSendContext.reset();
                processModeThreadLocal.remove();
                // 累加delay次数
                keyValueManager.increment(messageDelayAndRouteBackStdQueueCountKey, 1,
                                          ShuffleConstants.DUPLICATE_CHECK_VALID_PERIOD);

            }

        }

        private static Object doRealProcess(Object[] args, Invoker invoker, KeyValueManager keyValueManager,
                                            String messageOwnerEnvKey, String curEnv) {

            // 真正消费前，声明环境占用。避免不必要的跨环境竞争,避免业务执行时间较长导致std环境做delay处理。
            try {
                String ownerEnv = keyValueManager.getValue(messageOwnerEnvKey);
                if (!curEnv.equals(ownerEnv)) {
                    keyValueManager.setValue(messageOwnerEnvKey, curEnv, ShuffleConstants.DUPLICATE_CHECK_VALID_PERIOD);
                }
            } finally {
                return invoker.invoke(args);
            }

        }

        private static Object processForStd(Message message, String stdEnv, List<String> originAndStdEnvs,
                                            String currentApplicationName, KeyValueManager keyValueManager,
                                            String messageOwnerEnvKey, String messageDelayAndRouteBackStdQueueCountKey,
                                            EnvironmentDetector environmentDetector,
                                            InterProcessMutexContext queueConsumerMutexContext, String curEnv,
                                            String messageKey, Object[] args, Invoker invoker, Object proxy) {
            // 当前为基准环境
            int caseToProcess = getCaseForStdConsumer(message, stdEnv, originAndStdEnvs, currentApplicationName,
                                                      keyValueManager, messageOwnerEnvKey,
                                                      messageDelayAndRouteBackStdQueueCountKey, environmentDetector);
            if (DELAY == caseToProcess) {

                return doDelay(args, invoker, keyValueManager, messageDelayAndRouteBackStdQueueCountKey, proxy,
                               message);

            } else if (PROCESS == caseToProcess) {

                InterProcessMutex lock = queueConsumerMutexContext.getLock(messageOwnerEnvKey);
                boolean acquired = false;
                try {
                    acquired = lock.acquire(3600, TimeUnit.SECONDS);
                } catch (Exception e) {
                    return doDelay(args, invoker, keyValueManager, messageDelayAndRouteBackStdQueueCountKey, proxy,
                                   message);
                }
                // 长时间仍锁定超时，丢弃该消息。即由能获取锁定的消费者处理(基准环境）
                if (!acquired) {
                    return doIgnore(args, invoker, proxy);
                }
                // 获取锁后
                try {

                    // 二次检查
                    int caseToProcessSecondTimeCheck = getCaseForStdConsumer(message, stdEnv, originAndStdEnvs,
                                                                             currentApplicationName, keyValueManager,
                                                                             messageOwnerEnvKey,
                                                                             messageDelayAndRouteBackStdQueueCountKey,
                                                                             environmentDetector);
                    if (PROCESS == caseToProcessSecondTimeCheck) {
                        // 执行消息消费
                        return doRealProcess(args, invoker, keyValueManager, messageOwnerEnvKey, curEnv);
                    } else if (DELAY == caseToProcessSecondTimeCheck) {
                        return doDelay(args, invoker, keyValueManager, messageDelayAndRouteBackStdQueueCountKey, proxy,
                                       message);
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
                return processForStd(message, stdEnv, originAndStdEnvs, currentApplicationName, keyValueManager,
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

        private static String getShuffleSendId(Message message) {
            String shuffleSendId = null;
            if (message != null && message.getMessageProperties() != null) {
                shuffleSendId = (String) message.getMessageProperties().getHeaders().get(ShuffleRabbitConstants.HeaderName.SHUFFLE_SEND_ID_HEADER_NAME);
            }
            if (shuffleSendId == null) {
                shuffleSendId = "NullSendId";
            }
            return shuffleSendId;
        }

        /**
         * traceId-queueName-md5(body)
         * 
         * @param message
         * @return
         */
        private static String getMessageIdKey(Message message) {
            try {
                return Tracing.currentTracer().currentSpan().context().traceIdString() + "-"
                       + getShuffleSendId(message);
            } catch (Exception e) {
                return UUID.randomUUID().toString().replaceAll("-", "");
            }

        }

        private static RabbitTemplate getRabbitSenderOnSameBroker(Object proxy) {
            if (proxy instanceof AbstractMessageListenerContainer) {
                AbstractMessageListenerContainer listenerContainer = (AbstractMessageListenerContainer) proxy;
                RabbitTemplate dleSender = new RabbitTemplate(listenerContainer.getConnectionFactory());
                return dleSender;
            }
            return null;

        }

        public static Object getMessageListener(Object[] args, Invoker invoker, Object proxy) throws Exception {

            Integer processMode = processModeThreadLocal.get();

            Object retObject = invoker.invoke(args);

            if (new Integer(DELAY).equals(processMode)) {

                boolean delaySucessfullyBySend = false;

                RabbitTemplate dleSender = getRabbitSenderOnSameBroker(proxy);
                RabbitShuffleSendContext.DelaySendContext delaySendContext = RabbitShuffleSendContext.getDelaySendContextThreadLocal().get();
                if (delaySendContext != null && delaySendContext.getMessage() != null && dleSender != null) {
                    Message originMessage = delaySendContext.getMessage();
                    // raw queue name
                    String delayRoutingKey = ShuffleEnvironmentInfoProcessUtils.getRawNameWithoutCurrentEnvironmentInfo(originMessage.getMessageProperties().getConsumerQueue());
                    if (StringUtils.isNotBlank(delayRoutingKey)) {
                        dleSender.send(ShuffleRabbitConstants.SHUFFLE_DELAY_EXCHANGE, delayRoutingKey, originMessage);
                        delaySucessfullyBySend = true;
                    }

                }

                return getMockedMessageLister(!delaySucessfullyBySend, retObject);

            } else if (new Integer(IGNORE).equals(processMode)) {
                return getMockedMessageLister(false, retObject);
            }
            return retObject;

        }

        static String errorMessage = "reject caused by current shuffled consumer can't decide yet, wait next ttl comes to decide.";

        /**
         * @param rejectAndDontRequeue true：通过异常，触发死信。false： NOOP
         * @param originMessageListener
         * @return
         */
        private static Object getMockedMessageLister(boolean rejectAndDontRequeue, Object originMessageListener) {
            if (originMessageListener instanceof MessageListener) {
                return new MessageListener() {

                    @Override
                    public void onMessage(Message message) {
                        if (rejectAndDontRequeue) {
                            throw new AmqpRejectAndDontRequeueException(errorMessage);
                        }
                    }
                };

            } else if (originMessageListener instanceof ChannelAwareMessageListener) {
                return new ChannelAwareMessageListener() {

                    @Override
                    public void onMessage(Message message, Channel channel) throws Exception {

                        if (rejectAndDontRequeue) {
                            throw new AmqpRejectAndDontRequeueException(errorMessage);
                        }
                    }
                };
            }
            return originMessageListener;
        }

        @RuntimeType
        public static Object intercept(@AllArguments Object[] args, @Morph Invoker invoker, @Origin Method method,
                                       @This Object proxy) throws Exception {

            // do for "getMessageListener" method
            if (method.getName().equals("getMessageListener")) {
                return getMessageListener(args, invoker, proxy);
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
                SpanUtils.tagPropagationInfos(consumerSpan);
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
