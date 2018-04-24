package com.netease.edu.eds.trace.support;/**
 * Created by hzfjd on 18/4/19.
 */

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
}
