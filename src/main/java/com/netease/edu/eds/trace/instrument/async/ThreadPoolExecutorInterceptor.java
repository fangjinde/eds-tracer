package com.netease.edu.eds.trace.instrument.async;

import java.util.concurrent.Callable;

import org.springframework.cloud.sleuth.DefaultSpanNamer;
import org.springframework.cloud.sleuth.ErrorParser;
import org.springframework.cloud.sleuth.SpanNamer;

import com.netease.edu.eds.trace.constants.SpanType;
import com.netease.edu.eds.trace.support.EduExceptionMessageErrorParser;
import com.netease.edu.eds.trace.support.SpringBeanFactorySupport;
import com.netease.edu.eds.trace.utils.ExceptionHandler;

/**
 * @author hzfjd
 * @create 18/7/2
 **/
public class ThreadPoolExecutorInterceptor {

    private static final SpanNamer   DefaultSpanNamer   = new DefaultSpanNamer();
    private static final ErrorParser DefaultErrorParser = new EduExceptionMessageErrorParser();

    public static void intercept(Object[] args, Callable<Void> originalCall, Object proxy) {

        AsyncTracing asyncTracing = SpringBeanFactorySupport.getBean(AsyncTracing.class);
        if (asyncTracing == null) {
            try {
                originalCall.call();
                return;
            } catch (Exception e) {
                throw ExceptionHandler.wrapToRuntimeException(e);
            }
        }

        if (AsyncTracedMarkContext.isTraced()) {
            try {
                originalCall.call();
                return;
            } catch (Exception e) {
                throw ExceptionHandler.wrapToRuntimeException(e);
            }
        } else {
            try {
                AsyncTracedMarkContext.markTraced();
                SpanNamer spanNamer = SpringBeanFactorySupport.getBean(SpanNamer.class);
                if (spanNamer == null) {
                    spanNamer = DefaultSpanNamer;
                }
                ErrorParser errorParser = SpringBeanFactorySupport.getBean(ErrorParser.class);
                if (errorParser == null) {
                    errorParser = DefaultErrorParser;
                }

                // magic here. just wrapper the args will work!
                args[0] = new EduTraceRunnable(asyncTracing.tracing().tracer(), spanNamer, errorParser,
                                               (Runnable) args[0], SpanType.AsyncSubType.NATIVE_THREAD_POOL);
                try {
                    originalCall.call();
                    return;
                } catch (Exception e) {
                    throw ExceptionHandler.wrapToRuntimeException(e);
                }

            } finally {
                AsyncTracedMarkContext.reset();
            }
        }

    }
}
