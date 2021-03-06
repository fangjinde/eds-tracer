/*
 * Copyright 2013-2018 the original author or authors. Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the
 * License.
 */

package com.netease.edu.eds.trace.instrument.async;

import brave.Span;
import brave.Tracer;
import com.netease.edu.eds.trace.constants.SpanType;
import com.netease.edu.eds.trace.utils.SpanUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.cloud.sleuth.SpanNamer;
import org.springframework.cloud.sleuth.TraceKeys;
import org.springframework.cloud.sleuth.util.SpanNameUtil;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;

/**
 * Aspect that creates a new Span for running threads executing methods annotated with
 * {@link org.springframework.scheduling.annotation.Async} annotation.
 *
 * @author Marcin Grzejszczak
 * @since 1.0.0
 * @see Tracer
 */
@Aspect
public class EduTraceAsyncAspect {

    private final Tracer    tracer;
    private final SpanNamer spanNamer;
    private final TraceKeys traceKeys;

    public EduTraceAsyncAspect(Tracer tracer, SpanNamer spanNamer, TraceKeys traceKeys) {
        this.tracer = tracer;
        this.spanNamer = spanNamer;
        this.traceKeys = traceKeys;
    }

    @Around("execution (@org.springframework.scheduling.annotation.Async  * *.*(..))")
    public Object traceBackgroundThread(final ProceedingJoinPoint pjp) throws Throwable {

        Span span = this.tracer.currentSpan();
        // 异步只做衔接，不发追踪发起。否则会导致无意义的追踪的信息太多。
        // 异步追踪，如果之前没有追踪上下文则不新起追踪.
        if (span == null) {
            return pjp.proceed();
        }

        String spanName = this.spanNamer.name(getMethod(pjp, pjp.getTarget()),
                                              SpanNameUtil.toLowerHyphen(SpanType.AsyncSubType.SPRING_ASYNC + ":"
                                                                         + pjp.getTarget().getClass().getSimpleName()
                                                                         + "." + pjp.getSignature().getName()));
        span.name(spanName);
        try (Tracer.SpanInScope ws = this.tracer.withSpanInScope(span)) {
            SpanUtils.safeTag(span, this.traceKeys.getAsync().getPrefix() + this.traceKeys.getAsync().getClassNameKey(),
                              pjp.getTarget().getClass().getSimpleName());
            SpanUtils.safeTag(span,
                              this.traceKeys.getAsync().getPrefix() + this.traceKeys.getAsync().getMethodNameKey(),
                              pjp.getSignature().getName());
            return pjp.proceed();
        } finally {
            span.finish();
        }
    }

    private Method getMethod(ProceedingJoinPoint pjp, Object object) {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        return ReflectionUtils.findMethod(object.getClass(), method.getName(), method.getParameterTypes());
    }

}
