package com.netease.edu.eds.shuffle.instrument.rabbit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import com.netease.edu.eds.shuffle.support.QueueShuffleUtils;

/**
 * 匿名queue不做任何处理。 收集所有环境的裸queueNames列表。 仅对非std环境有效。 对所有的queue的declare和registerBean做args的替换，增加ttl，dle，expire
 * （仅对原无参数的queue有效），并且dlrk为裸queue名称。 通过BeanPostProcessor和RabbitAdmin实现（目前主要是spring cloud stream
 * rabbit会先rabbitAdmin.declare）。
 * 
 * @author hzfjd
 * @create 18/8/23
 **/
public class RabbitQueueArgumentsCustomBPP implements BeanPostProcessor {

    private static final Logger logger = LoggerFactory.getLogger(RabbitQueueArgumentsCustomBPP.class);

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return null;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {

        if (!(bean instanceof Queue)) {
            return bean;
        }

        Queue queue = (Queue) bean;
        QueueShuffleUtils.addQueueArgumentsForShuffle(queue);

        return bean;
    }

}
