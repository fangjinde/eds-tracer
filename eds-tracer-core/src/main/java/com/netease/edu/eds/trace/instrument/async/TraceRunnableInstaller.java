package com.netease.edu.eds.trace.instrument.async;

import brave.Tracer;
import org.springframework.cloud.sleuth.ErrorParser;
import org.springframework.cloud.sleuth.SpanNamer;

import java.util.concurrent.Callable;

public interface TraceRunnableInstaller {
    void install(Object[] args, Callable<Void> originalCall, Object proxy, Tracer tracer, SpanNamer spanNamer, ErrorParser errorParser);
}
