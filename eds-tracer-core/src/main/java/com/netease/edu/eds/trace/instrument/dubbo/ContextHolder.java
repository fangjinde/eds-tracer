package com.netease.edu.eds.trace.instrument.dubbo;

/**
 * @author hzfjd
 * @create 19/1/18
 **/
public class ContextHolder {

    private static final ThreadLocal<DubboInvokeTraceContext> contextThreadLocal = ThreadLocal.withInitial(() -> new DubboInvokeTraceContext());

    public static DubboInvokeTraceContext get() {
        return contextThreadLocal.get();
    }

    public static void set(DubboInvokeTraceContext context) {
        contextThreadLocal.set(context);
    }

    public static void reset() {
        contextThreadLocal.remove();
    }

}
