package com.netease.edu.eds.trace.utils;

import brave.Span;
import brave.Tracer;
import brave.Tracing;

/**
 * @author hzfjd
 * @create 19/1/18
 **/
public class TraceUtils {

    /**
     * 获取traceId
     * 
     * @return
     */
    public static String getTraceIdString() {
        Tracer tracer = Tracing.currentTracer();
        if (tracer == null) {
            return null;
        }
        Span span = tracer.currentSpan();
        if (span == null) {
            return null;
        }

        if (span.context() == null) {
            return null;
        }

        return span.context().traceIdString();

    }
}
