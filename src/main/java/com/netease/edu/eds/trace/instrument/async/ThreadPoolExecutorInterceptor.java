package com.netease.edu.eds.trace.instrument.async;

import com.netease.edu.eds.trace.utils.ClassUtils;
import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.SuperCall;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

/**
 * @author hzfjd
 * @create 18/6/27
 **/
public class ThreadPoolExecutorInterceptor {

    public static void execute(@Argument(0) Runnable command, @SuperCall Callable<Void> executorService) {
        System.out.println("execute before:" + command + executorService);

        try {


            Class<?> revertClass = Class.forName("com.netease.edu.boot.hystrixtest.ByteBuddyTest", true,
                                                 ClassUtils.getClassLoader());
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
}
