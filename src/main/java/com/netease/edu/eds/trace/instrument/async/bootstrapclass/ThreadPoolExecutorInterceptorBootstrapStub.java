package com.netease.edu.eds.trace.instrument.async.bootstrapclass;

import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;

/**
 * 本类需要bootstrapClassLoader来加载。因此最好不要依赖非bootstrap类，否则还要处理一些额外的类路径变更事宜。
 * 
 * @author hzfjd
 * @create 18/6/27
 **/
public class ThreadPoolExecutorInterceptorBootstrapStub {

    public static void execute(@Argument(0) Runnable command, @SuperCall Callable<Void> originalCall,
                               @Origin Method originMethod, @This Object proxy) {

        try {
            Class revertClass = Class.forName("com.netease.edu.eds.trace.instrument.async.ThreadPoolExecutorInterceptor",
                                              true, getClassLoader());
            Method revertMethod = revertClass.getMethod("execute", Runnable.class, Callable.class, Method.class,
                                                        Object.class);
            revertMethod.invoke(null, command, originalCall, originMethod, proxy);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException
                | InvocationTargetException e) {
            throw new RuntimeException("ThreadPoolExecutorInterceptorBootstrapStub execute error.", e);
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
