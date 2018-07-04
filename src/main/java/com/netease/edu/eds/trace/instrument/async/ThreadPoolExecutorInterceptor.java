package com.netease.edu.eds.trace.instrument.async;

import com.netease.edu.eds.trace.support.SpringBeanFactorySupport;
import com.netease.edu.eds.trace.utils.ExceptionHandler;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

/**
 * @author hzfjd
 * @create 18/7/2
 **/
public class ThreadPoolExecutorInterceptor {

    public static void execute(Runnable command, Callable<Void> originalCall, Method originMethod, Object proxy) {

        AsyncTracing asyncTracing = SpringBeanFactorySupport.getBean(AsyncTracing.class);
        if (asyncTracing == null) {
            try {
                originalCall.call();
                return;
            } catch (Exception e) {
                throw ExceptionHandler.wrapToRuntimeException(e);
            }
        }


        try {
            originalCall.call();
            return;
        } catch (Exception e) {
            throw ExceptionHandler.wrapToRuntimeException(e);
        }

        // Span asyncSpan = asyncTracing.tracing().tracer().nextSpan();
        //
        // try (Tracer.SpanInScope spanInScope = asyncTracing.tracing().tracer().withSpanInScope(asyncSpan)) {
        // asyncSpan.kind(Span.Kind.CLIENT).name("async").start();
        //
        // originMethod.invoke(proxy, asyncTracing.tracing().currentTraceContext().wrap(command));
        //
        // return;
        //
        // } catch (Exception e) {
        // throw ExceptionHandler.wrapToRuntimeException(e);
        // } finally {
        // DdbTraceContext.setSpan(null);
        // asyncSpan.finish();
        // }

    }
}
