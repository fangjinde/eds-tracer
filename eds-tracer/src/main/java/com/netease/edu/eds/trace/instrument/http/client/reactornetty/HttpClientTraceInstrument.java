package com.netease.edu.eds.trace.instrument.http.client.reactornetty;

import brave.Span;
import brave.Tracer;
import brave.Tracing;
import brave.propagation.TraceContextOrSamplingFlags;
import com.netease.edu.eds.trace.core.Invoker;
import com.netease.edu.eds.trace.support.AbstractTraceAgentInstrumetation;
import com.netease.edu.eds.trace.utils.SpanUtils;
import io.netty.handler.codec.http.HttpMethod;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bind.annotation.*;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.utility.JavaModule;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.ipc.netty.http.client.HttpClientRequest;
import reactor.ipc.netty.http.client.HttpClientResponse;
import reactor.util.context.Context;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * @author hzfjd
 * @create 19/1/29
 **/
public class HttpClientTraceInstrument extends AbstractTraceAgentInstrumetation {

	@Override
	protected ElementMatcher.Junction defineTypeMatcher(Map<String, String> props) {
		return namedIgnoreCase("reactor.ipc.netty.http.client.HttpClient");
	}

	@Override
	protected ElementMatcher.Junction defineMethodMatcher(Map<String, String> props,
			TypeDescription typeDescription, ClassLoader classLoader, JavaModule module) {
		// public Mono<HttpClientResponse> request(HttpMethod method, String url,
		// Function<? super HttpClientRequest, ? extends Publisher<Void>> handler)
		ElementMatcher.Junction request3 = isDeclaredBy(typeDescription)
				.and(namedIgnoreCase("request")).and(isPublic()).and(takesArguments(3));
		return request3;
	}

	@Override
	protected Class defineInterceptorClass(Map<String, String> props) {
		return Interceptor.class;
	}

	public static class Interceptor {

		private static final Logger log = LoggerFactory.getLogger(Interceptor.class);
		private final static String CONTEXT_ERROR = "sleuth.webfilter.context.error";

		@RuntimeType
		public static Object intercept(@AllArguments Object[] args,
				@Morph Invoker invoker, @Origin Method method, @This Object proxy) {

			if (new Random().nextInt(10) % 2 == 1) {
				return intercept1(args, invoker, method, proxy);
			}
			else {
				return intercept2(args, invoker, method, proxy);
			}

		}

		private static Object intercept2(Object[] args, Invoker invoker, Method method,
				Object proxy) {

			Tracer tracer = Tracing.currentTracer();
			if (tracer == null) {
				return invoker.invoke(args);
			}

			Object retObject = null;

			HttpMethod httpMethod = (HttpMethod) args[0];
			String url = (String) args[1];
			Function<? super HttpClientRequest, ? extends Publisher<Void>> handler = (Function<? super HttpClientRequest, ? extends Publisher<Void>>) args[2];
			retObject = invoker.invoke(args);

			if (retObject instanceof Mono) {

				String spanName = httpMethod.name() + url;
				Mono<HttpClientResponse> monoReponse = (Mono<HttpClientResponse>) retObject;
				AtomicReference<Span> spanRef = new AtomicReference<>();

				Mono<HttpClientResponse> tracedMono = monoReponse.doOnError(t -> {
					Span span = spanRef.get();
					SpanUtils.tagErrorMark(span);
					SpanUtils.tagError(span, t);

				}).doOnTerminate(() -> {
					Span span = spanRef.get();
					if (span != null) {
						span.finish();
					}
				}).subscriberContext(doTraceBeforeRequest(tracer, spanName, spanRef));

				return tracedMono;
			}

			return retObject;

		}

		private static Object intercept1(Object[] args, Invoker invoker, Method method,
				Object proxy) {
			Tracer tracer = Tracing.currentTracer();
			if (tracer == null) {
				return invoker.invoke(args);
			}

			Object retObject = null;

			HttpMethod httpMethod = (HttpMethod) args[0];
			String url = (String) args[1];
			Function<? super HttpClientRequest, ? extends Publisher<Void>> handler = (Function<? super HttpClientRequest, ? extends Publisher<Void>>) args[2];
			AtomicReference<HttpClientResponse> httpClientResponseRef = new AtomicReference();
			retObject = invoker.invoke(args);

			if (retObject instanceof Mono) {

				String spanName = httpMethod.name() + url;
				Mono<HttpClientResponse> monoReponse = (Mono<HttpClientResponse>) retObject;

				Mono<HttpClientResponse> tracedMono = monoReponse.map(res -> {
					httpClientResponseRef.set(res);
					return res;
				}).compose(p -> p.then(Mono.subscriberContext()).onErrorResume(
						t -> Mono.subscriberContext().map(c -> c.put(CONTEXT_ERROR, t)))
						.flatMap(doTraceAfterResponse(tracer,
								httpClientResponseRef.get())))
						.subscriberContext(doTraceBeforeRequest(tracer, spanName, null));

				return tracedMono;
			}

			return retObject;
		}

		private static Function<? super Context, ? extends Mono<? extends HttpClientResponse>> doTraceAfterResponse(
				Tracer tracer, HttpClientResponse httpClientResponse) {
			return context -> {

				Mono responseMono = Mono.fromSupplier(() -> httpClientResponse);
				Span span = null;
				if (context.hasKey(Span.class)) {
					span = context.get(Span.class);
				}
				if (span == null) {
					span = tracer.currentSpan();
				}

				if (span == null) {
					return responseMono;
				}
				Throwable t = null;
				if (context.hasKey(CONTEXT_ERROR)) {
					t = context.get(CONTEXT_ERROR);
					SpanUtils.tagError(span, t);
					SpanUtils.tagErrorMark(span);

				}
				else {

				}

				span.finish();
				return responseMono;

			};

		}

		private static Function<Context, Context> doTraceBeforeRequest(Tracer tracer,
				String spanName, AtomicReference<Span> spanRef) {
			return (c) -> {

				Span span;
				Span parentSpan = null;
				if (c.hasKey(Span.class)) {
					parentSpan = c.get(Span.class);
				}
				if (parentSpan == null) {
					parentSpan = tracer.currentSpan();
				}

				if (parentSpan == null) {
					span = tracer.nextSpan();
				}
				else {
					span = tracer.nextSpan(
							TraceContextOrSamplingFlags.create(parentSpan.context()));
				}

				span.name(spanName).start();

				if (spanRef != null) {
					spanRef.set(span);
				}

				return c.put(Span.class, span);

			};
		}
	}
}
