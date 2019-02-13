package com.netease.edu.eds.trace.instrument.async.bootstrapclass;

import com.netease.edu.eds.trace.core.Invoker;

import java.util.concurrent.Callable;

/**
 * @author hzfjd
 * @create 18/7/16
 **/
public class BootstrapInterceptorSupport {

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
    public static ClassLoader getClassLoader() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader != null) {
            return classLoader;
        }
        return ClassLoader.getSystemClassLoader();

    }

}
