package com.netease.edu.eds.trace.instrument.async;

/**
 * 多种异步追踪实现会在不同层进行追踪包装，通过该本地线程变量进行标记，防止重复包装。
 * 
 * @author hzfjd
 * @create 18/5/11
 **/
public class AsyncTracedMarkContext {

    private static ThreadLocal<Boolean> tracedMarkThreadLocal = new ThreadLocal();

    public static void markTraced() {
        tracedMarkThreadLocal.set(true);
    }

    public static void reset() {
        tracedMarkThreadLocal.remove();
    }

    public static Boolean isTraced() {
        Boolean tracedMark = tracedMarkThreadLocal.get();
        return tracedMark != null && tracedMark.booleanValue();
    }

}
