/*
 * Copyright 2013-2018 the original author or authors. Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the
 * License.
 */

package com.netease.edu.eds.trace.instrument.async;

import org.springframework.cloud.sleuth.ErrorParser;
import org.springframework.cloud.sleuth.SpanNamer;

import com.netease.edu.eds.trace.constants.SpanType;
import com.netease.edu.eds.trace.utils.SpanUtils;

import brave.Span;
import brave.Tracer;
import brave.Tracer.SpanInScope;

/**
 * Runnable that passes Span between threads. The Span name is taken either from the passed value or from the
 * {@link SpanNamer} interface.
 *
 * @author Spencer Gibb
 * @author Marcin Grzejszczak
 * @since 1.0.0
 */
public class EduTraceRunnable implements Runnable {

    /**
     * Since we don't know the exact operation name we provide a default name for the Span
     */
    private static final String DEFAULT_SPAN_NAME = "async";

    private final Tracer        tracer;
    private final Runnable      delegate;
    private final Span          span;
    private final ErrorParser   errorParser;
    private String              asyncType;

    public EduTraceRunnable(Tracer tracer, SpanNamer spanNamer, ErrorParser errorParser, Runnable delegate) {
        this(tracer, spanNamer, errorParser, delegate, null);
    }

    public EduTraceRunnable(Tracer tracer, SpanNamer spanNamer, ErrorParser errorParser, Runnable delegate,
                            String name) {
        this.tracer = tracer;
        this.delegate = delegate;
        this.asyncType = name;
        // 异步只做衔接，不发追踪发起。否则会导致无意义的追踪的信息太多。
        // 异步追踪，如果之前没有追踪上下文则不新起追踪.
        Span currentSpan = this.tracer.currentSpan();
        if (currentSpan != null) {
            String spanName = name != null ? name : spanNamer.name(delegate, DEFAULT_SPAN_NAME);
            this.span = this.tracer.nextSpan().name(spanName);
        }

        this.errorParser = errorParser;
    }

    @Override
    public void run() {
        // 异步只做衔接，不发追踪发起。否则会导致无意义的追踪的信息太多。
        // 异步追踪，如果之前没有追踪上下文则不新起追踪.
        if (span == null) {
            this.delegate.run();
            return;
        }

        Throwable error = null;
        try (SpanInScope ws = this.tracer.withSpanInScope(this.span.start())) {
            SpanUtils.safeTag(span, SpanType.AsyncSubType.TAG_KEY, asyncType);
            this.delegate.run();
        } catch (RuntimeException | Error e) {
            error = e;
            throw e;
        } finally {
            this.errorParser.parseErrorTags(this.span, error);
            this.span.finish();
        }
    }
}
