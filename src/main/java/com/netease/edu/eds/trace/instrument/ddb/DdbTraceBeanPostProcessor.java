package com.netease.edu.eds.trace.instrument.ddb;/**
 * Created by hzfjd on 18/3/23.
 */

import com.netease.dbsupport.transaction.IDBTransactionManager;
import com.netease.framework.dbsupport.SqlManager;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author hzfjd
 * @create 18/3/23
 */
public class DdbTraceBeanPostProcessor extends InstantiationAwareBeanPostProcessorAdapter {

    private final Set<Object> earlyProxyReferences =
            Collections.newSetFromMap(new ConcurrentHashMap<Object, Boolean>(16));

    @Autowired
    private DdbTracing ddbTracing;

    @Override
    public Object getEarlyBeanReference(Object bean, String beanName) throws BeansException {

        Object cacheKey = getCacheKey(bean.getClass(), beanName);
        if (!this.earlyProxyReferences.contains(cacheKey)) {
            this.earlyProxyReferences.add(cacheKey);
        }
        return createTraceWrapperIfNecessary(bean, beanName);

    }

    @Override public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean == null) {
            return bean;
        }
        if (bean instanceof FactoryBean) {
            return bean;
        }

        Object cacheKey = getCacheKey(bean.getClass(), beanName);
        if (!this.earlyProxyReferences.contains(cacheKey)) {
            return createTraceWrapperIfNecessary(bean, beanName);
        }

        return bean;
    }

    private Object createTraceWrapperIfNecessary(Object bean, String beanName) {

        if (bean instanceof SqlManager && !(bean instanceof SqlManagerTraceWrapper)) {
            return new SqlManagerTraceWrapper((SqlManager) bean, ddbTracing);
        }

        if (bean instanceof IDBTransactionManager && !(bean instanceof IDBTransactionManagerTraceWrapper)) {
            return new IDBTransactionManagerTraceWrapper((IDBTransactionManager) bean, ddbTracing);
        }

        return bean;
    }

    /**
     * Build a cache key for the given bean class and bean name.
     * <p>Note: As of 4.2.3, this implementation does not return a concatenated
     * class/name String anymore but rather the most efficient cache key possible:
     * a plain bean name, prepended with {@link org.springframework.beans.factory.BeanFactory#FACTORY_BEAN_PREFIX}
     * in case of a {@code FactoryBean}; or if no bean name specified, then the
     * given bean {@code Class} as-is.
     *
     * @param beanClass the bean class
     * @param beanName  the bean name
     * @return the cache key for the given class and name
     */
    private Object getCacheKey(Class<?> beanClass, String beanName) {
        if (StringUtils.hasLength(beanName)) {
            return (FactoryBean.class.isAssignableFrom(beanClass) ?
                    BeanFactory.FACTORY_BEAN_PREFIX + beanName : beanName);
        } else {
            return beanClass;
        }
    }

}
