package com.netease.edu.eds.trace.instrument.rabbit;/**
 * Created by hzfjd on 18/4/13.
 */

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;

/**
 * @author hzfjd
 * @create 18/4/13
 */
public class RabbitTraceBeanFactoryPostProcessor implements BeanDefinitionRegistryPostProcessor {

    private static final String RABBIT_TEMPLATE_CLASS_NAME                   = "org.springframework.amqp.rabbit.core.RabbitTemplate";
    private static final String SIMPLE_MESSAGE_LISTENER_CONTAINER_CLASS_NAME = "org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer";

    @Override public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {

        for (String beanName : registry.getBeanDefinitionNames()) {
            BeanDefinition beanDefinition = registry.getBeanDefinition(beanName);
            if (RABBIT_TEMPLATE_CLASS_NAME.equals(beanDefinition.getBeanClassName())) {
                if (beanDefinition instanceof AbstractBeanDefinition) {
                    AbstractBeanDefinition abstractBeanDefinition = (AbstractBeanDefinition) beanDefinition;
                    if (abstractBeanDefinition.hasBeanClass()) {
                        abstractBeanDefinition.setBeanClass(TracedRabbitTemplate.class);
                    }
                    abstractBeanDefinition.setBeanClassName(TracedRabbitTemplate.class.getName());
                }
            }
            if (SIMPLE_MESSAGE_LISTENER_CONTAINER_CLASS_NAME.equals(beanDefinition.getBeanClassName())) {
                if (beanDefinition instanceof AbstractBeanDefinition) {
                    AbstractBeanDefinition abstractBeanDefinition = (AbstractBeanDefinition) beanDefinition;
                    if (abstractBeanDefinition.hasBeanClass()) {
                        abstractBeanDefinition.setBeanClass(SimpleTracedMessageListenerContainer.class);
                    }
                    abstractBeanDefinition.setBeanClassName(SimpleTracedMessageListenerContainer.class.getName());
                }
            }

        }
    }

    @Override public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

    }
}
