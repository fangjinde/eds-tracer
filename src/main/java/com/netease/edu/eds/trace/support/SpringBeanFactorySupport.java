package com.netease.edu.eds.trace.support;/**
                                           * Created by hzfjd on 18/4/19.
                                           */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;

import java.util.Optional;

/**
 * @author hzfjd
 * @create 18/4/19
 */
public class SpringBeanFactorySupport implements BeanFactoryAware {

    private volatile static BeanFactory s_beanFactory = null;

    private static final Logger         logger        = LoggerFactory.getLogger(SpringBeanFactorySupport.class);

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

    public static <T> T getBean(Class<T> requiredType) {
        if (s_beanFactory == null) {
            return null;
        }
        try {
            return s_beanFactory.getBean(requiredType);
        } catch (Exception e) {
            logger.warn("getBean error:", e);
            return null;
        }
    }
}
