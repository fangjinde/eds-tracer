package com.netease.edu.eds.trace.demo.ioc;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;

import java.util.Arrays;
import java.util.List;

/**
 * @author hzfjd
 * @create 18/5/17
 **/
public class ExcludeBeanFactoryPostProcessor implements BeanDefinitionRegistryPostProcessor {

    List<String> excludeBeanNames = Arrays.asList("com.netease.edu.persist.service.aop.TransactionAspect");

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {

        for (String beanName : registry.getBeanDefinitionNames()) {

            BeanDefinition beanDefinition = registry.getBeanDefinition(beanName);
            if (StringUtils.isNotBlank(beanDefinition.getBeanClassName())) {
                if (excludeBeanNames.contains(beanDefinition.getBeanClassName())) {
                    registry.removeBeanDefinition(beanName);
                }
            }
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

    }
}
