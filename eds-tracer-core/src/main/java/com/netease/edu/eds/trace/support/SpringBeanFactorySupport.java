package com.netease.edu.eds.trace.support;/**
                                           * Created by hzfjd on 18/4/19.
                                           */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.Environment;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 实现BeanDefinitionRegistryPostProcessor和PriorityOrdered仅仅为了确保在普通单例实现前初始化。
 * 
 * @author hzfjd
 * @create 18/4/19
 */
public class SpringBeanFactorySupport implements BeanFactoryAware, BeanDefinitionRegistryPostProcessor, PriorityOrdered {

    private volatile static BeanFactory                    s_beanFactory = null;

    private static final Logger                            logger        = LoggerFactory.getLogger(SpringBeanFactorySupport.class);

    private static final ConcurrentHashMap<Class, Object>  byTypeCache   = new ConcurrentHashMap();

    private static final ConcurrentHashMap<String, Object> byNameCache   = new ConcurrentHashMap();

    private static final Object                            NULL_OBJECT   = new Object();

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        s_beanFactory = beanFactory;
    }

    public static BeanFactory getBeanFactory() {
        return s_beanFactory;
    }

    public static Environment getEnvironment() {
        return getBean(Environment.class);
    }

    public static Optional<BeanFactory> getBeanFactorySafely() {
        return Optional.ofNullable(s_beanFactory);
    }

    public static <T> T getBean(String beanName) {
        if (s_beanFactory == null) {
            return null;
        }

        Object bean = byNameCache.get(beanName);
        if (bean != null) {
            if (NULL_OBJECT == bean) {
                return null;
            }
            return (T) bean;
        }

        try {
            bean = s_beanFactory.getBean(beanName);
        } catch (Exception e) {
            logger.error("getBean error:", e);
        }

        if (bean == null) {
            byNameCache.putIfAbsent(beanName, NULL_OBJECT);
            return null;
        }

        Object previousBean = byNameCache.putIfAbsent(beanName, bean);
        if (previousBean != null) {
            return (T) previousBean;
        }
        return (T) bean;
    }

    /**
     * 注意：设计为只支持单例。 增加缓存，降低getBean开销
     * 
     * @param requiredType
     * @param <T>
     * @return
     */
    public static <T> T getBean(Class<T> requiredType) {
        if (s_beanFactory == null) {
            return null;
        }

        Object bean = byTypeCache.get(requiredType);
        if (bean != null) {
            if (NULL_OBJECT == bean) {
                return null;
            }
            return (T) bean;
        }

        try {
            bean = s_beanFactory.getBean(requiredType);
        } catch (Exception e) {
            logger.error("getBean error:", e);
        }

        if (bean == null) {
            byTypeCache.putIfAbsent(requiredType, NULL_OBJECT);
            return null;
        }

        Object previousBean = byTypeCache.putIfAbsent(requiredType, bean);
        if (previousBean != null) {
            return (T) previousBean;
        }
        return (T) bean;

    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {

    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 2;
    }

}
