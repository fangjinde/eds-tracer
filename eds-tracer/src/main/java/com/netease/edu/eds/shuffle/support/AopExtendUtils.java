package com.netease.edu.eds.shuffle.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;

/**
 * Aop工具类
 */
public class AopExtendUtils {

    private static final Logger logger = LoggerFactory.getLogger(AopExtendUtils.class);

    /**
     * 获取被AOP代理的对象
     *
     * @param bean
     * @param <T>
     * @return
     * @throws Exception
     */
    public static final <T> T unwrapProxy(T bean) throws Exception {
        if (AopUtils.isAopProxy(bean) && bean instanceof Advised) {
            Advised advised = (Advised) bean;
            bean = (T) advised.getTargetSource().getTarget();
        }
        return bean;
    }

    public static final <T> T unwrapProxySafely(T bean) {
        T beanRet = bean;
        try {
            beanRet = unwrapProxy(bean);
        } catch (Exception e) {
            logger.error("unwrapProxy failed, using origin bean. ", e);
        }
        return beanRet;
    }

}
