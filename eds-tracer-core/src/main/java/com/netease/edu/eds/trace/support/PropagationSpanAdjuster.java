package com.netease.edu.eds.trace.support;

import brave.propagation.ExtraFieldPropagation;
import org.springframework.cloud.sleuth.SpanAdjuster;
import zipkin2.Span;

import java.io.Closeable;
import java.util.Map;

/**
 * @author hzfjd
 * @create 18/8/22
 **/
public class PropagationSpanAdjuster implements SpanAdjuster {

    @Override
    public Span adjust(Span span) {

        Map<String, String> propagationExtraMap = ExtraFieldPropagation.getAll();
        if (propagationExtraMap == null) {
            return span;
        }

        for (Map.Entry<String, String> entry : propagationExtraMap.entrySet()) {

        }

        return null;
    }

    public static void main(String[] args) {
        try (SpanInScope scope = withScope()) {
            System.out.println("biz do");
        } finally {
            System.out.println("finally do");
        }
    }

    private static SpanInScope withScope() {
        return new SpanInScope();
    }

    public static class SpanInScope implements Closeable {

        public SpanInScope() {
        }

        @Override
        public void close() {
            System.out.println("scope closed.");
        }

    }
}
