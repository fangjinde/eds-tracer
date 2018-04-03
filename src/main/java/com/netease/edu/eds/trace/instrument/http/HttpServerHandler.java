package com.netease.edu.eds.trace.instrument.http;

import brave.Span;
import brave.Tracer;
import brave.http.HttpSampler;
import brave.http.HttpServerAdapter;
import brave.http.HttpServerParser;
import brave.http.HttpTracing;
import brave.internal.Nullable;
import brave.propagation.TraceContext;
import brave.propagation.TraceContextOrSamplingFlags;
import zipkin2.Endpoint;

import javax.servlet.http.HttpServletRequest;

/**
 * This standardizes a way to instrument http servers, particularly in a way that encourages use of
 * portable customizations via {@link brave.http.HttpServerParser}.
 * <p>This is an example of synchronous instrumentation:
 * <pre>{@code
 * Span span = handler.handleReceive(extractor, request);
 * Throwable error = null;
 * try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
 *   // any downstream code can see Tracer.currentSpan() or use Tracer.currentSpanCustomizer()
 *   response = invoke(request);
 * } catch (RuntimeException | Error e) {
 *   error = e;
 *   throw e;
 * } finally {
 *   handler.handleSend(response, error, span);
 * }
 * }</pre>
 *
 * @param <Req>  the native http request type of the server.
 * @param <Resp> the native http response type of the server.
 */
public final class HttpServerHandler<Req, Resp> {

    public static <Req, Resp> HttpServerHandler<Req, Resp> create(HttpTracing httpTracing,
                                                                  HttpServerAdapter<Req, Resp> adapter,
                                                                  WebDebugMatcher webDebugMatcher) {
        return new HttpServerHandler<>(
                httpTracing.tracing().tracer(),
                httpTracing.serverSampler(),
                httpTracing.serverParser(),
                adapter, webDebugMatcher
        );
    }

    final   Tracer                       tracer;
    final   HttpSampler                  sampler;
    final   HttpServerParser             parser;
    final   HttpServerAdapter<Req, Resp> adapter;
    private WebDebugMatcher              webDebugMatcher;

    HttpServerHandler(
            Tracer tracer,
            HttpSampler sampler,
            HttpServerParser parser,
            HttpServerAdapter<Req, Resp> adapter, WebDebugMatcher webDebugMatcher
    ) {
        this.tracer = tracer;
        this.sampler = sampler;
        this.parser = parser;
        this.adapter = adapter;
        this.webDebugMatcher = webDebugMatcher;
    }

    /**
     * Conditionally joins a span, or starts a new trace, depending on if a trace context was
     * extracted from the request. Tags are added before the span is started.
     * <p>This is typically called before the request is processed by the actual library.
     */
    public Span handleReceive(TraceContext.Extractor<Req> extractor, Req request) {
        return handleReceive(extractor, request, request);
    }

    /**
     * Like {@link #handleReceive(brave.propagation.TraceContext.Extractor, Object)}, except for when the carrier of
     * trace data is not the same as the request.
     * <p>Request data is parsed before the span is started.
     *
     * @see brave.http.HttpServerParser#request(brave.http.HttpAdapter, Object, brave.SpanCustomizer)
     */
    public <C> Span handleReceive(TraceContext.Extractor<C> extractor, C carrier, Req request) {
        Span span = nextSpan(extractor.extract(carrier), request);
        if (span.isNoop()) return span;

        // all of the parsing here occur before a timestamp is recorded on the span
        span.kind(Span.Kind.SERVER);
        parseRequest(request, span);

        return span.start();
    }

    void parseRequest(Req request, Span span) {
        if (span.isNoop()) return;
        // Ensure user-code can read the current trace context
        Tracer.SpanInScope ws = tracer.withSpanInScope(span);
        try {
            parser.request(adapter, request, span.customizer());
        } finally {
            ws.close();
        }

        Endpoint.Builder remoteEndpoint = Endpoint.newBuilder();
        if (adapter.parseClientAddress(request, remoteEndpoint)) {
            span.remoteEndpoint(remoteEndpoint.build());
        }
    }

    /**
     * Creates a potentially noop span representing this request
     */
    Span nextSpan(TraceContextOrSamplingFlags extracted, Req request) {
        Boolean sampled = null;
        if (extracted.sampled() == null) { // Otherwise, try to make a new decision
            if (request instanceof HttpServletRequest) {
                if (webDebugMatcher.matches((HttpServletRequest) request)) {
                    sampled = true;
                }
            }
            extracted = extracted.sampled(sampled);
        }
        Span span = extracted.context() != null
                ? tracer.joinSpan(extracted.context())
                : tracer.nextSpan(extracted);
        if (sampled != null && sampled) {
            span.tag("DebugMark", webDebugMatcher.debugMark());
        }
        return span;
    }

    /**
     * Finishes the server span after assigning it tags according to the response or error.
     * <p>This is typically called once the response headers are sent, and after the span is {@link
     * brave.Tracer.SpanInScope#close() no longer in scope}.
     *
     * @see brave.http.HttpServerParser#response(brave.http.HttpAdapter, Object, Throwable, brave.SpanCustomizer)
     */
    public void handleSend(@Nullable Resp response, @Nullable Throwable error, Span span) {
        if (span.isNoop()) return;

        // Ensure user-code can read the current trace context
        Tracer.SpanInScope ws = tracer.withSpanInScope(span);
        try {
            parser.response(adapter, response, error, span.customizer());
        } finally {
            ws.close();
            span.finish();
        }
    }
}
