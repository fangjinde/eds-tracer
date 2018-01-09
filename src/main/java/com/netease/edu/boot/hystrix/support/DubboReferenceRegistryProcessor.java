package com.netease.edu.boot.hystrix.support;/**
 * Created by hzfjd on 18/1/9.
 */

import com.alibaba.dubbo.config.spring.ReferenceBean;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;

/**
 * 修改dubbo ReferenceBean为Primary. 后续注解版本的dubbo,可能有需求调整的地方,再说吧.
 * @author hzfjd
 * @create 18/1/9
 */
public class DubboReferenceRegistryProcessor implements BeanDefinitionRegistryPostProcessor {

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        for (String beanName : registry.getBeanDefinitionNames()) {
            BeanDefinition bdf= registry.getBeanDefinition(beanName);
            AbstractBeanDefinition rootBeanDefinition=null;
            if (bdf instanceof AbstractBeanDefinition){
                rootBeanDefinition=(AbstractBeanDefinition)bdf;
                if(rootBeanDefinition.hasBeanClass()&&ReferenceBean.class.equals(rootBeanDefinition.getBeanClass())){
                    rootBeanDefinition.setPrimary(true);
                }
            }
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

    }
}
