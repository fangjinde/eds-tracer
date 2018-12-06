package com.netease.edu.eds.trace.instrument.jobservice;

/**
 * @author hzfjd
 * @create 18/10/24
 **/
public class JobServiceDeplaySendTraceContext {

    private static final ThreadLocal<Boolean> s_deplaySendMarkHolder = new ThreadLocal<>();

    public static void mark() {
        s_deplaySendMarkHolder.set(true);
    }

    public static boolean inDelaySend() {
        return Boolean.TRUE.equals(s_deplaySendMarkHolder.get());
    }

    public static void reset() {
        s_deplaySendMarkHolder.remove();
    }
}
