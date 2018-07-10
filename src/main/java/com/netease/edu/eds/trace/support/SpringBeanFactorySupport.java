package com.netease.edu.eds.trace.support;/**
                                           * Created by hzfjd on 18/4/19.
                                           */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author hzfjd
 * @create 18/4/19
 */
public class SpringBeanFactorySupport implements BeanFactoryAware {

    private volatile static BeanFactory                   s_beanFactory = null;

    private static final Logger                           logger        = LoggerFactory.getLogger(SpringBeanFactorySupport.class);

    private static final ConcurrentHashMap<Class, Object> cache         = new ConcurrentHashMap();

    private static final Object                           NULL_OBJECT   = new Object();

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        s_beanFactory = beanFactory;
    }

    public static BeanFactory getBeanFactory() {
        return s_beanFactory;
    }

    public static Optional<BeanFactory> getBeanFactorySafely() {
        return Optional.ofNullable(s_beanFactory);
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

        Object bean = cache.get(requiredType);
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
            cache.putIfAbsent(requiredType, NULL_OBJECT);
            return null;
        }

        Object previousBean = cache.putIfAbsent(requiredType, bean);
        if (previousBean != null) {
            return (T) previousBean;
        }
        return (T) bean;

    }
}
