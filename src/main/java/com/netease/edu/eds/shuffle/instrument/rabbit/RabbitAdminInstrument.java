package com.netease.edu.eds.shuffle.instrument.rabbit;

import com.alibaba.fastjson.JSON;
import com.netease.edu.eds.shuffle.core.BeanNameConstants;
import com.netease.edu.eds.shuffle.core.EnvironmentShuffleUtils;
import com.netease.edu.eds.shuffle.core.ShufflePropertiesSupport;
import com.netease.edu.eds.shuffle.core.ShuffleRabbitConstants;
import com.netease.edu.eds.shuffle.support.*;
import com.netease.edu.eds.trace.core.Invoker;
import com.netease.edu.eds.trace.spi.TraceAgentInstrumetation;
import com.netease.edu.eds.trace.support.AgentSupport;
import com.netease.edu.eds.trace.support.DefaultAgentBuilderListener;
import com.netease.edu.eds.trace.support.SpringBeanFactorySupport;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ShutdownSignalException;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bind.annotation.*;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ChannelProxy;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.util.ReflectionUtils;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * @author hzfjd
 * @create 18/8/9
 **/
public class RabbitAdminInstrument implements TraceAgentInstrumetation {

    private ElementMatcher.Junction getMethodMatcher(TypeDescription typeDescription) {
        ElementMatcher.Junction matcher1 = isDeclaredBy(typeDescription).and(namedIgnoreCase("declareQueues"));
        ElementMatcher.Junction matcher2 = isDeclaredBy(typeDescription).and(namedIgnoreCase("declareBindings"));
        ElementMatcher.Junction matcher3 = isDeclaredBy(typeDescription).and(namedIgnoreCase("declareQueue").and(takesArguments(1)));
        return matcher1.or(matcher2).or(matcher3);
    }

    private ElementMatcher.Junction getClassMatcher() {
        return namedIgnoreCase("org.springframework.amqp.rabbit.core.RabbitAdmin");
    }

    @Override
    public void premain(Map<String, String> props, Instrumentation inst) {
        new AgentBuilder.Default().type(getClassMatcher()).transform((builder, typeDescription, classloader,
                                                                      javaModule) -> builder.method(getMethodMatcher(typeDescription)).intercept(AgentSupport.getInvokerMethodDelegationCustomer().to(Interceptor.class))).with(DefaultAgentBuilderListener.getInstance()).installOn(inst);

    }

    public static class Interceptor {
        // private DeclareOk[] declareQueues(final Channel channel, final Queue... queues) throws IOException

        private static Logger                  logger                                 = LoggerFactory.getLogger(Interceptor.class);

        private static AtomicReference<Method> logOrRethrowDeclarationExceptionMethod = new AtomicReference<>();

        @RuntimeType
        public static Object intercept(@AllArguments Object[] args, @Morph Invoker invoker, @Origin Method method,
                                       @This Object proxy) throws IOException {

            if (method.getName().equals("declareQueues")) {
                return declareQueues(args, invoker, method, proxy);
            } else if (method.getName().equals("declareQueue")) {
                return declareQueue(args, invoker, method, proxy);
            }
            return declareBindings(args, invoker, method, proxy);
        }

        /**
         * 代理单个queue的declare
         * 
         * @param args
         * @param invoker
         * @param method
         * @param proxy
         * @return
         */
        private static Object declareQueue(Object[] args, Invoker invoker, Method method, Object proxy) {
            Queue queue = (Queue) args[0];
            // shuffle开关打开时，对queue的参数进行定制化
            QueueShuffleUtils.addQueueArgumentsForShuffle(queue);

            final Object result;
            try {
                result = declareQueueWithRetryAfterDelete(queue, new Callable() {

                    @Override
                    public Object call() {
                        return invoker.invoke(args);
                    }
                }, new Runnable() {

                    @Override
                    public void run() {
                        if (proxy instanceof RabbitAdmin) {
                            RabbitAdmin rabbitAdmin = (RabbitAdmin) proxy;
                            rabbitAdmin.deleteQueue(queue.getName());
                        }
                    }
                });
            } catch (Exception e) {
                if (e instanceof AmqpException) {
                    throw (AmqpException) e;
                } else {
                    throw new AmqpException("declareQueue with new Arguments error, queue:" + JSON.toJSONString(queue),
                                            e);
                }
            }

            return result;
        }

        /**
         * queue参数变更导致declare异常的重试逻辑
         * 
         * @param queue
         * @param originDeclareOps
         * @param deleteOps
         * @return
         * @throws Exception
         */

        private static Object declareQueueWithRetryAfterDelete(Queue queue, Callable originDeclareOps,
                                                               Runnable deleteOps) throws Exception {
            int triedTimes = 1;
            Exception lastException = null;
            do {

                try {

                    InterProcessMutexContext queueRedeclareMutexContext = SpringBeanFactorySupport.getBean(BeanNameConstants.QUEUE_REDECLARE_MUTEX_CONTEXT);
                    InterProcessMutex lock = queueRedeclareMutexContext.getLock(queue.getName());
                    // 第二次开始进行删除
                    boolean acquired = false;
                    try {
                        acquired = lock.acquire(600, TimeUnit.SECONDS);
                        if (!acquired) {
                            logger.error("try lock failed. just do the job what so ever.");
                        }
                    } catch (Exception e) {
                        logger.error("try lock failed. just do the job what so ever.");
                    }
                    // 锁定成功或锁定超时，都进行操作
                    try {
                        if (triedTimes >= 2) {
                            deleteOps.run();
                        }
                        return originDeclareOps.call();
                        // 没有异常则创建成功，跳出循环
                    } finally {
                        try {
                            if (acquired = true) {
                                lock.release();
                            }
                        } catch (Exception e) {
                            logger.error("release lock failed. nothing left to do.");
                        }
                    }

                } catch (Exception e) {
                    lastException = e;
                    if (isContainInequivalentArgExceptionCause(e)) {
                        // 符合重试条件
                        triedTimes++;
                        try {
                            Thread.sleep(1000L);
                        } catch (InterruptedException e1) {

                        }
                    } else {
                        // 不符合重试条件，直接抛异常
                        throw e;
                    }

                }

            } while (triedTimes <= 3);

            // 重试2次后仍然失败，抛最后一个异常
            if (lastException != null) {
                throw lastException;
            }
            return null;

        }

        /**
         * 未抽取公共重试逻辑的历史方法，保留一段时间
         * 
         * @param channel
         * @param queue
         * @param declareOks
         * @throws IOException
         */
        @Deprecated
        private static void innerDeclareQueueWithRetry(Channel channel, Queue queue,
                                                       List<AMQP.Queue.DeclareOk> declareOks) throws IOException {
            int triedTimes = 1;
            IOException lastException = null;
            do {

                try {

                    InterProcessMutexContext queueRedeclareMutexContext = SpringBeanFactorySupport.getBean(BeanNameConstants.QUEUE_REDECLARE_MUTEX_CONTEXT);
                    InterProcessMutex lock = queueRedeclareMutexContext.getLock(queue.getName());
                    // 第二次开始进行删除
                    boolean acquired = false;
                    try {
                        acquired = lock.acquire(600, TimeUnit.SECONDS);
                        if (!acquired) {
                            logger.error("try lock failed. just do the job what so ever.");
                        }
                    } catch (Exception e) {
                        logger.error("try lock failed. just do the job what so ever.");
                    }
                    // 锁定成功或锁定超时，都进行操作
                    try {
                        if (triedTimes >= 2) {
                            channel.queueDelete(queue.getName());
                        }
                        innerDeclareQueue(channel, queue, declareOks);
                        // 没有异常则创建成功，跳出循环
                        return;
                    } finally {
                        try {
                            if (acquired = true) {
                                lock.release();
                            }
                        } catch (Exception e) {
                            logger.error("release lock failed. nothing left to do.");
                        }
                    }

                } catch (IOException e) {
                    lastException = e;
                    if (isContainInequivalentArgExceptionCause(e)) {
                        // 符合重试条件
                        triedTimes++;
                        try {
                            Thread.sleep(1000L);
                        } catch (InterruptedException e1) {

                        }
                    } else {
                        // 不符合重试条件，直接抛异常
                        throw e;
                    }

                }

            } while (triedTimes <= 3);

            // 重试2次后仍然失败，抛最后一个异常
            if (lastException != null) {
                throw lastException;
            }

        }

        /**
         * 批量声明队列中源码逻辑副本
         * 
         * @param channel
         * @param queue
         * @param declareOks
         * @throws IOException
         */
        private static void innerDeclareQueue(Channel channel, Queue queue,
                                              List<AMQP.Queue.DeclareOk> declareOks) throws IOException {
            try {
                AMQP.Queue.DeclareOk declareOk = channel.queueDeclare(queue.getName(), queue.isDurable(),
                                                                      queue.isExclusive(), queue.isAutoDelete(),
                                                                      queue.getArguments());
                declareOks.add(declareOk);
            } catch (IllegalArgumentException e) {
                if (logger.isDebugEnabled()) {
                    logger.error("Exception while declaring queue: '" + queue.getName() + "'");
                }
                try {
                    if (channel instanceof ChannelProxy) {
                        ((ChannelProxy) channel).getTargetChannel().close();
                    }
                } catch (TimeoutException e1) {
                }
                throw new IOException(e);
            }
        }

        /**
         * 代理批量queue declare
         * 
         * @param args
         * @param invoker
         * @param method
         * @param proxy
         * @return
         * @throws IOException
         */
        private static Object declareQueues(Object[] args, Invoker invoker, Method method,
                                            Object proxy) throws IOException {

            Channel channel = (Channel) args[0];

            Queue[] queues = (Queue[]) args[1];

            List<AMQP.Queue.DeclareOk> declareOks = new ArrayList<AMQP.Queue.DeclareOk>(queues.length);
            for (int i = 0; i < queues.length; i++) {
                Queue queue = queues[i];

                if (!queue.getName().startsWith("amq.")) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("declaring Queue '" + queue.getName() + "'");
                    }
                    // shuffle开关打开时，对queue的参数进行定制化
                    QueueShuffleUtils.addQueueArgumentsForShuffle(queue);

                    try {

                        declareQueueWithRetryAfterDelete(queue, new Callable() {

                            @Override
                            public Object call() throws Exception {
                                innerDeclareQueue(channel, queue, declareOks);
                                return null;
                            }
                        }, new Runnable() {

                            @Override
                            public void run() {
                                try {
                                    channel.queueDelete(queue.getName());
                                } catch (Exception e) {
                                    logger.error("delete queue error. just ignore and retry. with queue:"
                                                 + JSON.toJSONString(queue));
                                }

                            }
                        });

                    } catch (Exception e) {
                        // 异常，并且不符合重试重试，或重试后仍然异常
                        logOrRethrowDeclarationException(args, invoker, proxy);

                    }
                } else if (logger.isDebugEnabled()) {
                    logger.debug(queue.getName() + ": Queue with name that starts with 'amq.' cannot be declared.");
                }
            }
            return declareOks.toArray(new AMQP.Queue.DeclareOk[declareOks.size()]);

        }

        private static boolean isContainInequivalentArgExceptionCause(Throwable e) {
            Throwable curException = e;
            while (curException != null) {
                if (curException instanceof ShutdownSignalException
                    && curException.getMessage().contains("inequivalent arg")) {
                    return true;
                }
                curException = curException.getCause();
            }
            return false;
        }

        /**
         * 通过反射调用源码对应日志和异常处理方法
         * 
         * @param args
         * @param invoker
         * @param proxy
         * @throws IOException
         */
        private static void logOrRethrowDeclarationException(Object[] args, Invoker invoker,

                                                             Object proxy) throws IOException {
            // protected void actualInvokeListener(Channel channel, Message message) throws Exception
            Method method = logOrRethrowDeclarationExceptionMethod.get();
            if (method == null) {
                method = ReflectionUtils.findMethod(RabbitAdmin.class, "logOrRethrowDeclarationException",
                                                    Declarable.class, String.class, Throwable.class);
                if (method != null) {
                    ReflectionUtils.makeAccessible(method);
                }
                logOrRethrowDeclarationExceptionMethod.set(method);
            }

            if (method == null) {
                logger.error("miss logOrRethrowDeclarationException method, using origin method. which might cause unknown errors.");
                invoker.invoke(args);
                return;
            }

            try {
                method.invoke(proxy, args);
            } catch (IllegalAccessException e) {
                logger.error("IllegalAccessException while calling logOrRethrowDeclarationException method, using origin method. which might cause unknown errors.");
                invoker.invoke(args);
                return;
            } catch (InvocationTargetException e) {
                if (e.getTargetException() instanceof IOException) {
                    throw (IOException) e.getTargetException();
                } else {
                    throw new IOException("InvocationTargetException not IOException case compatible",
                                          e.getTargetException());
                }

            }
        }

        // private void declareBindings(final Channel channel, final Binding... bindings) throws IOException
        private static Object declareBindings(Object[] args, Invoker invoker, Method method,
                                              Object proxy) throws IOException {

            Binding[] bindings = (Binding[]) args[1];
            RabbitAdmin rabbitAdmin = (RabbitAdmin) proxy;

            for (Binding originBinding : bindings) {

                if (QueueShuffleUtils.isShuffleInnerExchange(originBinding.getExchange())) {
                    continue;
                }

                // 裸queueName做为RouteBackRoutingKey。
                String routeBackRoutingKey = ShuffleEnvironmentInfoProcessUtils.getRawNameWithoutCurrentEnvironmentInfo(originBinding.getDestination());

                // 注册表里面的queue都是非匿名queue。因此不用额外查询所有Queue Bean进行过滤。
                if (!NamedQueueRawNameRegistry.contain(routeBackRoutingKey)) {
                    continue;
                }

                // 替换为对应std环境的queueName
                String relatedStdQueueName = originBinding.getDestination();
                if (!ShufflePropertiesSupport.getStandardEnvName().equals(EnvironmentShuffleUtils.getCurrentEnv())) {
                    relatedStdQueueName = ShuffleEnvironmentInfoProcessUtils.getNameWithNewFixOrRemoveOldFix(originBinding.getDestination(),
                                                                                                             EnvironmentShuffleUtils.getCurrentEnv(),
                                                                                                             ShufflePropertiesSupport.getStandardEnvName());
                }

                Binding routeBackBinding = new Binding(relatedStdQueueName, originBinding.getDestinationType(),
                                                       ShuffleRabbitConstants.SHUFFLE_ROUTE_BACK_EXCHANGE,
                                                       routeBackRoutingKey, null);

                BindingResitry.add(new BindingResitry.BindingHolder(routeBackBinding, rabbitAdmin));

            }

            return invoker.invoke(args);
        }
    }
}
