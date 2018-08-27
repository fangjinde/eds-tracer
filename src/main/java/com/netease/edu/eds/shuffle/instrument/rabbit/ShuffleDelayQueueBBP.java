package com.netease.edu.eds.shuffle.instrument.rabbit;

import com.netease.edu.eds.shuffle.core.ShuffleRabbitConstants;
import org.springframework.amqp.core.*;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 新增一个delayExchange（名称和binding固定），一个delayQueue（名称和dle固定，dlrk不定制），一个shuffle.route.back.exchange <!-- shuffle 延迟exchange
 * --> <rabbit:topic-exchange name="shuffle.delay.exchange" durable="true" auto-delete="false"> <rabbit:bindings>
 * <rabbit:binding queue="shuffle.delay.queue" pattern="#"/> </rabbit:bindings> </rabbit:topic-exchange> ====== <!--
 * shuffle 延迟queue --> <rabbit:queue id="shuffle.delay.queue" durable="true" auto-delete="false" exclusive="false" name=
 * "shuffle.delay.queue"> <rabbit:queue-arguments>
 * <entry key="x-dead-letter-exchange" value="shuffle.route.back.exchange"/>
 * <!-- 延迟3小时后再由原业务重试。正常情况下，走到延迟队列的都不需要及时处理。重试次数4*重试间隔3h=12小时不存在的，则std接管处理。-->
 * <entry key="x-message-ttl" value="180000" value-type="java.lang.Long"/> </rabbit:queue-arguments> </rabbit:queue>
 *
 * @author hzfjd
 * @create 18/8/24
 **/
public class ShuffleDelayQueueBBP implements BeanFactoryPostProcessor {

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        Exchange delayExchange = new TopicExchange(ShuffleRabbitConstants.SHUFFLE_DELAY_EXCHANGE, true, false);
        beanFactory.registerSingleton(ShuffleRabbitConstants.SHUFFLE_DELAY_EXCHANGE, delayExchange);

        Map<String, Object> queueArguments = new LinkedHashMap<>();
        queueArguments.put(ShuffleRabbitConstants.ParamName.X_DEAD_LETTER_EXCHANGE,
                           ShuffleRabbitConstants.SHUFFLE_ROUTE_BACK_EXCHANGE);
        queueArguments.put(ShuffleRabbitConstants.ParamName.X_MESSAGE_TTL, 180000L);
        Queue delayQueue = new Queue(ShuffleRabbitConstants.SHUFFLE_DELAY_QUEUE, true, false, false, queueArguments);
        beanFactory.registerSingleton(ShuffleRabbitConstants.SHUFFLE_DELAY_QUEUE, delayQueue);

        Binding actualBinding = new Binding(ShuffleRabbitConstants.SHUFFLE_DELAY_QUEUE, Binding.DestinationType.QUEUE,
                                            ShuffleRabbitConstants.SHUFFLE_DELAY_EXCHANGE, "#", null);

        beanFactory.registerSingleton(ShuffleRabbitConstants.SHUFFLE_DELAY_EXCHANGE + ".bind."
                                      + ShuffleRabbitConstants.SHUFFLE_DELAY_QUEUE, actualBinding);

        Exchange routeBackExchange = new DirectExchange(ShuffleRabbitConstants.SHUFFLE_ROUTE_BACK_EXCHANGE, true,
                                                        false);
        beanFactory.registerSingleton(ShuffleRabbitConstants.SHUFFLE_ROUTE_BACK_EXCHANGE, routeBackExchange);
    }
}
