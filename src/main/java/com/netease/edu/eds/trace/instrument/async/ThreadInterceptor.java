package com.netease.edu.eds.trace.instrument.async;

import brave.Tracer;
import com.netease.edu.eds.trace.constants.SpanType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.sleuth.ErrorParser;
import org.springframework.cloud.sleuth.SpanNamer;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.concurrent.Callable;

/**
 * @author hzfjd
 * @create 18/7/16
 **/
public class ThreadInterceptor {

    private static Logger logger = LoggerFactory.getLogger(ThreadInterceptor.class);

    public static Object intercept(Object[] args, Callable<Void> originalCall, Object proxy) {

        TraceRunnableInstaller traceRunnableInstaller = new TraceRunnableInstaller() {

            @Override
            public void install(Object[] args, Callable<Void> originalCall, Object proxy, Tracer tracer,
                                SpanNamer spanNamer, ErrorParser errorParser) {
                if (!(proxy instanceof Thread)) {
                    return;
                }
                try {
                    Field field = Thread.class.getDeclaredField("target");
                    ReflectionUtils.makeAccessible(field);
                    Object targetValue = field.get(proxy);
                    if (targetValue instanceof Runnable) {
                        Runnable targetRunnable = (Runnable) targetValue;
                        field.set(proxy, new EduTraceRunnable(tracer, spanNamer, errorParser, targetRunnable,
                                                              SpanType.AsyncSubType.NATIVE_THREAD));

                    }

                } catch (Exception e) {
                    logger.error("cant install Thread Runnable trace", e);
                }
            }
        };

        return AsyncTraceInterceptorSupport.intercept(args, originalCall, proxy, traceRunnableInstaller);

    }

}
