package com.netease.edu.boot.hystrix.core;/**
 * Created by hzfjd on 18/1/5.
 */

import com.netease.edu.boot.hystrix.annotation.EduFallbackBean;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.core.annotation.AnnotationUtils;

import java.util.Map;

/**
 * @author hzfjd
 * @create 18/1/5
 */
public class SpringFallbackFactory implements FallbackFactory, BeanFactoryAware {

    private BeanFactory beanFactory;

    @Override
    public <T> T getFallback(Class<T> targetType) {
        return getFallback(targetType,false);
    }

    @Override
    public <T> T getFallback(Class<T> targetType, boolean providerSide) {
        if (beanFactory == null || !(beanFactory instanceof ListableBeanFactory)) {
            return null;
        }

        Map<String, T> beanMap = ((ListableBeanFactory) beanFactory).getBeansOfType(targetType);
        T fallback = null;
        for (Map.Entry<String, T> entry : beanMap.entrySet()) {
            EduFallbackBean eduFallbackBean = AnnotationUtils.findAnnotation(entry.getValue().getClass(),
                                                                             EduFallbackBean.class);
            if (eduFallbackBean != null&&eduFallbackBean.providerSide()==providerSide) {
                fallback = entry.getValue();
            }
        }

        return fallback;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }
}
