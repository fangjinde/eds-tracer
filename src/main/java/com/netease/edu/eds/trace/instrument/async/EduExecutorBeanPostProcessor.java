package com.netease.edu.eds.trace.instrument.async;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.framework.AopConfigException;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cloud.sleuth.instrument.async.LazyTraceExecutor;
import org.springframework.cloud.sleuth.instrument.async.LazyTraceThreadPoolTaskExecutor;
import org.springframework.core.PriorityOrdered;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.Executor;

/**
 * @author hzfjd
 * @create 18/7/9
 **/

public class EduExecutorBeanPostProcessor implements BeanPostProcessor, PriorityOrdered, BeanFactoryAware {

    private static final Log log = LogFactory.getLog(EduExecutorBeanPostProcessor.class);

    private BeanFactory      beanFactory;

    public EduExecutorBeanPostProcessor(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    public EduExecutorBeanPostProcessor(){

    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof Executor && !(bean instanceof ThreadPoolTaskExecutor)) {
            Method execute = ReflectionUtils.findMethod(bean.getClass(), "execute", Runnable.class);
            boolean methodFinal = Modifier.isFinal(execute.getModifiers());
            boolean classFinal = Modifier.isFinal(bean.getClass().getModifiers());
            boolean cglibProxy = !methodFinal && !classFinal;
            Executor executor = (Executor) bean;
            try {
                return createProxy(bean, cglibProxy, executor);
            } catch (AopConfigException e) {
                if (cglibProxy) {
                    if (log.isDebugEnabled()) {
                        log.debug("Exception occurred while trying to create a proxy, falling back to JDK proxy", e);
                    }
                    return createProxy(bean, false, executor);
                }
                throw e;
            }
        } else if (bean instanceof ThreadPoolTaskExecutor) {
            boolean classFinal = Modifier.isFinal(bean.getClass().getModifiers());
            boolean cglibProxy = !classFinal;
            ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) bean;
            return createThreadPoolTaskExecutorProxy(bean, cglibProxy, executor);
        }
        return bean;
    }

    Object createThreadPoolTaskExecutorProxy(Object bean, boolean cglibProxy, ThreadPoolTaskExecutor executor) {
        ProxyFactoryBean factory = new ProxyFactoryBean();
        factory.setProxyTargetClass(cglibProxy);
        factory.addAdvice(new ExecutorMethodInterceptor<ThreadPoolTaskExecutor>(executor, this.beanFactory) {

            @Override
            Executor executor(BeanFactory beanFactory, ThreadPoolTaskExecutor executor) {
                return new LazyTraceThreadPoolTaskExecutor(beanFactory, executor);
            }
        });
        factory.setTarget(bean);
        return factory.getObject();
    }

    @SuppressWarnings("unchecked")
    Object createProxy(Object bean, boolean cglibProxy, Executor executor) {
        ProxyFactoryBean factory = new ProxyFactoryBean();
        factory.setProxyTargetClass(cglibProxy);
        factory.addAdvice(new ExecutorMethodInterceptor(executor, this.beanFactory));
        factory.setTarget(bean);
        return factory.getObject();
    }

    @Override
    public int getOrder() {
        return PriorityOrdered.HIGHEST_PRECEDENCE + 1;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory=beanFactory;
    }

    public static class ExecutorMethodInterceptor<T extends Executor> implements MethodInterceptor {

        private final T           delegate;
        private final BeanFactory beanFactory;

        ExecutorMethodInterceptor(T delegate, BeanFactory beanFactory) {
            this.delegate = delegate;
            this.beanFactory = beanFactory;
        }

        @Override
        public Object invoke(MethodInvocation invocation) throws Throwable {
            Executor executor = executor(this.beanFactory, this.delegate);
            Method methodOnTracedBean = getMethod(invocation, executor);
            if (methodOnTracedBean != null) {
                return methodOnTracedBean.invoke(executor, invocation.getArguments());
            }
            return invocation.proceed();
        }

        private Method getMethod(MethodInvocation invocation, Object object) {
            Method method = invocation.getMethod();
            return ReflectionUtils.findMethod(object.getClass(), method.getName(), method.getParameterTypes());
        }

        Executor executor(BeanFactory beanFactory, T executor) {
            return new LazyTraceExecutor(beanFactory, executor);
        }
    }
}