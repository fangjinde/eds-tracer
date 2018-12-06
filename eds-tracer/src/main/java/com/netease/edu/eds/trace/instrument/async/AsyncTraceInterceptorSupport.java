package com.netease.edu.eds.trace.instrument.async;

import com.netease.edu.eds.trace.support.EduExceptionMessageErrorParser;
import com.netease.edu.eds.trace.support.SpringBeanFactorySupport;
import com.netease.edu.eds.trace.utils.ExceptionHandler;
import org.springframework.cloud.sleuth.DefaultSpanNamer;
import org.springframework.cloud.sleuth.ErrorParser;
import org.springframework.cloud.sleuth.SpanNamer;

import java.util.concurrent.Callable;

/**
 * @author hzfjd
 * @create 18/7/16
 **/
public class AsyncTraceInterceptorSupport {

    private static final SpanNamer   DefaultSpanNamer   = new DefaultSpanNamer();
    private static final ErrorParser DefaultErrorParser = new EduExceptionMessageErrorParser();

    public static Object intercept(Object[] args, Callable<Void> originalCall, Object proxy,
                                   TraceRunnableInstaller traceRunnableInstaller) {

        AsyncTracing asyncTracing = SpringBeanFactorySupport.getBean(AsyncTracing.class);
        // 异步只做衔接，不发追踪发起。否则会导致无意义的追踪的信息太多。
        // 异步追踪，如果之前没有追踪上下文则不新起追踪.
        if (asyncTracing == null || asyncTracing.tracing().tracer().currentSpan() == null) {
            try {
                return originalCall.call();
            } catch (Exception e) {
                throw ExceptionHandler.wrapToRuntimeException(e);
            }
        }

        if (AsyncTracedMarkContext.isTraced()) {
            try {
                return originalCall.call();
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

                if (traceRunnableInstaller != null) {
                    traceRunnableInstaller.install(args, originalCall, proxy, asyncTracing.tracing().tracer(),
                                                   spanNamer, errorParser);
                }

                try {
                    return originalCall.call();
                } catch (Exception e) {
                    throw ExceptionHandler.wrapToRuntimeException(e);
                }

            } finally {
                AsyncTracedMarkContext.reset();
            }
        }

    }

}
