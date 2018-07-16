package com.netease.edu.eds.trace.instrument.async.bootstrapclass;

import com.netease.edu.eds.trace.core.Invoker;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Morph;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;

/**
 * @author hzfjd
 * @create 18/7/16
 **/
public class ThreadBootstrapPatchStub {

    @RuntimeType
    public static Object intercept(@AllArguments Object[] args, @Morph Invoker invoker, @This Object proxy) {
        try {
            // 通过反射解决类命名空间隔离的问题
            Class revertClass = Class.forName("com.netease.edu.eds.trace.instrument.async.ThreadPoolExecutorInterceptor",
                                              true, getClassLoader());
            Method revertMethod = revertClass.getMethod("intercept", Object[].class, Callable.class, Object.class);
            // 通过Callable适配，解决Invoker类命名空间隔离的问题
            revertMethod.invoke(null, args, new OriginCall(args, invoker), proxy);
            return null;
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException
                | InvocationTargetException e) {
            throw new RuntimeException("ThreadPoolExecutorInterceptorBootstrapStub execute error.", e);
        }

    }

    public static class OriginCall implements Callable<Void> {

        Object[] args;
        Invoker  invoker;

        public OriginCall(Object[] args, Invoker invoker) {
            this.args = args;
            this.invoker = invoker;
        }

        @Override
        public Void call() throws Exception {
            invoker.invoke(args);
            return null;
        }
    }

    /**
     * 内部实现，不要依赖非bootstrap类
     *
     * @return
     */
    private static ClassLoader getClassLoader() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader != null) {
            return classLoader;
        }
        return ClassLoader.getSystemClassLoader();

    }
}
