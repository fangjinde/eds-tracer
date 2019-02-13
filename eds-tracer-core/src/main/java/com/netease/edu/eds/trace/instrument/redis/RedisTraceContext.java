package com.netease.edu.eds.trace.instrument.redis;

import brave.Span;

/**
 * @author hzfjd
 * @create 18/5/11
 **/
public class RedisTraceContext {

    private static ThreadLocal<Span> spanThreadLocal = new ThreadLocal<Span>();

    public static void setSpan(Span span) {
        spanThreadLocal.set(span);
    }

    public static Span currentSpan() {
        return spanThreadLocal.get();
    }

}
