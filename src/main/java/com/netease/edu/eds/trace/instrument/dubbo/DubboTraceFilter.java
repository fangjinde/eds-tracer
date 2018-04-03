package com.netease.edu.eds.trace.instrument.dubbo;

import brave.Span;
import brave.Span.Kind;
import brave.SpanCustomizer;
import brave.Tracer;
import brave.propagation.Propagation;
import brave.propagation.TraceContext;
import brave.propagation.TraceContextOrSamplingFlags;
import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.remoting.exchange.ResponseCallback;
import com.alibaba.dubbo.rpc.*;
import com.alibaba.dubbo.rpc.protocol.dubbo.FutureAdapter;
import com.alibaba.dubbo.rpc.support.RpcUtils;
import com.netease.edu.eds.trace.utils.ExceptionStringUtils;
import zipkin2.Endpoint;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.Future;

@Activate(group = { Constants.PROVIDER, Constants.CONSUMER }, order = -8990)
public final class DubboTraceFilter implements Filter {

    Tracer                                      tracer;
    TraceContext.Extractor<Map<String, String>> extractor;
    TraceContext.Injector<Map<String, String>>  injector;

    /**
     * {@link com.alibaba.dubbo.common.extension.ExtensionLoader} supplies the tracing implementation which must be named "tracing". For
     * example, if using the {@link com.alibaba.dubbo.config.spring.extension.SpringExtensionFactory}, only a bean named "tracing" will be
     * injected.
     */
    public void setDubboTracing(DubboTracing dubboTracing) {
        tracer = dubboTracing.tracing().tracer();
        extractor = dubboTracing.tracing().propagation().extractor(GETTER);
        injector = dubboTracing.tracing().propagation().injector(SETTER);
    }

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        if (tracer == null) return invoker.invoke(invocation);

        RpcContext rpcContext = RpcContext.getContext();
        Kind kind = rpcContext.isProviderSide() ? Kind.SERVER : Kind.CLIENT;
        final Span span;
        if (kind.equals(Kind.CLIENT)) {
            span = tracer.nextSpan();
            injector.inject(span.context(), invocation.getAttachments());
        } else {
            TraceContextOrSamplingFlags extracted = extractor.extract(invocation.getAttachments());
            span = extracted.context() != null
                    ? tracer.joinSpan(extracted.context())
                    : tracer.nextSpan(extracted);
        }

        if (!span.isNoop()) {
            span.kind(kind).start();
            String service = invoker.getInterface().getSimpleName();
            String method = RpcUtils.getMethodName(invocation);

            span.kind(kind);
            span.name(service + "/" + method);

            InetSocketAddress remoteAddress = rpcContext.getRemoteAddress();
            Endpoint.Builder remoteEndpoint = Endpoint.newBuilder().port(remoteAddress.getPort());
            if (!remoteEndpoint.parseIp(remoteAddress.getAddress())) {
                remoteEndpoint.parseIp(remoteAddress.getHostName());
            }

            Endpoint ep = remoteEndpoint.build();
            span.remoteEndpoint(ep);
        }

        boolean isOneway = false, deferFinish = false;
        try (Tracer.SpanInScope scope = tracer.withSpanInScope(span)) {
            Result result = invoker.invoke(invocation);
            if (result.hasException()) {
                onError(result.getException(), span);
            }
            isOneway = RpcUtils.isOneway(invoker.getUrl(), invocation);
            Future<Object> future = rpcContext.getFuture(); // the case on async client invocation
            if (future instanceof FutureAdapter) {
                deferFinish = true;
                ((FutureAdapter) future).getFuture().setCallback(new FinishSpanCallback(span));
            }
            return result;
        } catch (Error | RuntimeException e) {
            onError(e, span);
            throw e;
        } finally {
            if (isOneway) {
                span.flush();
            } else if (!deferFinish) {
                span.finish();
            }
        }
    }

    static void onError(Throwable error, SpanCustomizer span) {
        span.tag("has_error", String.valueOf(true));
        span.tag("dubbo_error", ExceptionStringUtils.getStackTraceString(error));
    }

    static final Propagation.Getter<Map<String, String>, String> GETTER =
            new Propagation.Getter<Map<String, String>, String>() {

                @Override
                public String get(Map<String, String> carrier, String key) {
                    return carrier.get(key);
                }

                @Override
                public String toString() {
                    return "Map::get";
                }
            };

    static final Propagation.Setter<Map<String, String>, String> SETTER =
            new Propagation.Setter<Map<String, String>, String>() {

                @Override
                public void put(Map<String, String> carrier, String key, String value) {
                    carrier.put(key, value);
                }

                @Override
                public String toString() {
                    return "Map::set";
                }
            };

    static final class FinishSpanCallback implements ResponseCallback {

        final Span span;

        FinishSpanCallback(Span span) {
            this.span = span;
        }

        @Override public void done(Object response) {
            span.finish();
        }

        @Override public void caught(Throwable exception) {
            onError(exception, span);
            span.finish();
        }
    }
}
