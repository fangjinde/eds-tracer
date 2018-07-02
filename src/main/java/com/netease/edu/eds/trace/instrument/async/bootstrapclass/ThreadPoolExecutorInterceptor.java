package com.netease.edu.eds.trace.instrument.async.bootstrapclass;

import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.SuperCall;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

/**
 * 本类需要bootstrapClassLoader来加载。因此最好不要依赖非bootstrap类，否则还要处理一些额外的类路径变更事宜。
 * 
 * @author hzfjd
 * @create 18/6/27
 **/
public class ThreadPoolExecutorInterceptor {

    public static void execute(@Argument(0) Runnable command, @SuperCall Callable<Void> executorService) {
        System.out.println("execute before:" + command + executorService);

        try {

            Class<?> revertClass = Class.forName("com.netease.edu.boot.hystrixtest.ByteBuddyTest", true,
                                                 getClassLoader());
            Method method = revertClass.getMethod("revertCall");
            System.out.println(method.invoke(null));

            executorService.call();
            System.out.println("executing:" + command + executorService);
        } catch (Exception e) {
            System.out.println("executing error:" + command + executorService);
            e.printStackTrace();
        }

        System.out.println("execute after:" + command + executorService);
    }

    public static void execute(@Argument(0) Runnable command) {
        System.out.println("execute before:" + command);

        System.out.println("execute after:" + command);
    }

    /**
     * 内部实现，不要依赖非bootstrap类
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
