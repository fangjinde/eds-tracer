package com.netease.edu.eds.shuffle.instrument.rabbit;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;

/**
 * @author hzfjd
 * @create 18/8/14
 **/
public class SpringRabbitShuffleCustomBeanFactoryPostProcessor implements BeanDefinitionRegistryPostProcessor {

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        for (String beanName : registry.getBeanDefinitionNames()) {
            BeanDefinition beanDefinition = registry.getBeanDefinition(beanName);
            if (TopicExchange.class.getName().equals(beanDefinition.getBeanClassName())) {
                processExchange(beanName, beanDefinition);
            }else if (FanoutExchange.class.getName().equals(beanDefinition.getBeanClassName())){
                processExchange(beanName, beanDefinition);
            }else if (DirectExchange.class.getName().equals(beanDefinition.getBeanClassName())){
                processExchange(beanName, beanDefinition);
            }else if (Queue.class.getName().equals(beanDefinition.getBeanClassName())){
                processQueue(beanName, beanDefinition);
            }else if (SimpleMessageListenerContainer.class.getName().equals(beanDefinition.getBeanClassName())){
                        //ListenerContainerFactoryBean
            }

        }
    }

    private void processQueue(String beanName, BeanDefinition beanDefinition) {
    }

    private void processExchange(String beanName, BeanDefinition beanDefinition) {
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

    }
}
