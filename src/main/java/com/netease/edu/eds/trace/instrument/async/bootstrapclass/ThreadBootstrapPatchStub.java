package com.netease.edu.eds.trace.instrument.async.bootstrapclass;

import com.netease.edu.eds.trace.core.Invoker;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Morph;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

/**
 * @author hzfjd
 * @create 18/7/16
 **/
public class ThreadBootstrapPatchStub {

    /**
     * 统一的拦截代码
     *
     * @param args
     * @param invoker
     * @param proxy
     * @return
     */
    @RuntimeType
    public static Object intercept(@AllArguments Object[] args, @Morph Invoker invoker, @This Object proxy) {
        try {
            // 通过反射解决类命名空间隔离的问题
            return reflectionCall(args, new BootstrapInterceptorSupport.OriginCall(args, invoker), proxy,
                                  BootstrapInterceptorSupport.getClassLoader());
        } catch (Exception e) {
            throw new RuntimeException("BootstrapInterceptorStub.reflectionCall error.", e);
        }

    }

    /**
     * 定制的反射调用部分
     *
     * @param args
     * @param callable
     * @param proxy
     * @param classLoader
     * @return
     * @throws Exception
     */

    private static Object reflectionCall(Object[] args, Callable callable, Object proxy,
                                         ClassLoader classLoader) throws Exception {

        // 通过反射解决类命名空间隔离的问题
        Class revertClass = Class.forName("com.netease.edu.eds.trace.instrument.async.ThreadInterceptor",
                                          true, classLoader);
        Method revertMethod = revertClass.getMethod("intercept", Object[].class, Callable.class, Object.class);
        // 通过Callable适配，解决Invoker类命名空间隔离的问题
        return revertMethod.invoke(null, args, callable, proxy);
    }
}
