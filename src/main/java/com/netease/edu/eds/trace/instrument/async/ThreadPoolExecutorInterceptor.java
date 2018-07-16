package com.netease.edu.eds.trace.instrument.async;

import brave.Tracer;
import com.netease.edu.eds.trace.constants.SpanType;
import org.springframework.cloud.sleuth.ErrorParser;
import org.springframework.cloud.sleuth.SpanNamer;

import java.util.concurrent.Callable;

/**
 * @author hzfjd
 * @create 18/7/2
 **/
public class ThreadPoolExecutorInterceptor {

    public static Object intercept(Object[] args, Callable<Void> originalCall, Object proxy) {
        TraceRunnableInstaller traceRunnableInstaller = new TraceRunnableInstaller() {

            @Override
            public void install(Object[] args, Callable<Void> originalCall, Object proxy, Tracer tracer,
                                SpanNamer spanNamer, ErrorParser errorParser) {
                // magic here. just wrapper the args will work!
                args[0] = new EduTraceRunnable(tracer, spanNamer, errorParser, (Runnable) args[0],
                                               SpanType.AsyncSubType.NATIVE_THREAD_POOL);
            }
        };
        return AsyncTraceInterceptorSupport.intercept(args, originalCall, proxy, traceRunnableInstaller);

    }
}
