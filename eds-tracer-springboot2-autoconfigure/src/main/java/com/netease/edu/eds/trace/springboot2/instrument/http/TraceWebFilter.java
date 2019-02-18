package com.netease.edu.eds.trace.springboot2.instrument.http;

import brave.Span;
import brave.Tracer;
import brave.http.HttpServerHandler;
import brave.http.HttpTracing;
import brave.propagation.Propagation;
import brave.propagation.SamplingFlags;
import brave.propagation.TraceContext;
import brave.propagation.TraceContextOrSamplingFlags;
import com.netease.edu.eds.trace.constants.CommonTagKeys;
import com.netease.edu.eds.trace.constants.SpanType;
import com.netease.edu.eds.trace.core.UrlParameterManagerDto;
import com.netease.edu.eds.trace.instrument.http.SkipUriMatcher;
import com.netease.edu.eds.trace.utils.EnvironmentUtils;
import com.netease.edu.eds.trace.utils.PropagationUtils;
import com.netease.edu.eds.trace.utils.SpanUtils;
import com.netease.edu.eds.trace.utils.TraceJsonUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A {@link WebFilter} that creates / continues / closes and detaches spans for a reactive
 * web application.
 *
 * @author Marcin Grzejszczak
 * @author hzfjd
 * @since 2.0.0
 */
public final class TraceWebFilter implements WebFilter, Ordered {

	private static final Log log = LogFactory
			.getLog(org.springframework.cloud.sleuth.instrument.web.TraceWebFilter.class);

	private static final String HTTP_COMPONENT = "http";
	protected static final String TRACE_REQUEST_ATTR = org.springframework.cloud.sleuth.instrument.web.TraceWebFilter.class
			.getName() + ".TRACE";
	private static final String TRACE_SPAN_WITHOUT_PARENT = org.springframework.cloud.sleuth.instrument.web.TraceWebFilter.class
			.getName() + ".SPAN_WITH_NO_PARENT";

	/**
	 * If you register your filter before the
	 * {@link org.springframework.cloud.sleuth.instrument.web.TraceWebFilter} then you
	 * will not have the tracing context passed for you out of the box. That means that
	 * e.g. your logs will not get correlated.
	 */
	public static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 5;

	static final Propagation.Getter<HttpHeaders, String> GETTER = new Propagation.Getter<HttpHeaders, String>() {

		@Override
		public String get(HttpHeaders carrier, String key) {
			return carrier.getFirst(key);
		}

		@Override
		public String toString() {
			return "HttpHeaders::getFirst";
		}
	};

	public static TraceWebFilter create(BeanFactory beanFactory,
			SkipUriMatcher skipUriMatcher) {
		return new TraceWebFilter(beanFactory, skipUriMatcher);
	}

	Tracer tracer;
	HttpServerHandler<ServerHttpRequest, ServerHttpResponse> handler;
	TraceContext.Extractor<HttpHeaders> extractor;
	private final BeanFactory beanFactory;
	private SkipUriMatcher skipUriMatcher;

	TraceWebFilter(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
		try {
			this.skipUriMatcher = beanFactory.getBean(SkipUriMatcher.class);
		}
		catch (BeansException e) {
			log.warn("no skipUriMatcher in beanFactory", e);
		}

	}

	TraceWebFilter(BeanFactory beanFactory, SkipUriMatcher skipUriMatcher) {
		this.beanFactory = beanFactory;
		this.skipUriMatcher = skipUriMatcher;
	}

	@SuppressWarnings("unchecked")
	HttpServerHandler<ServerHttpRequest, ServerHttpResponse> handler() {
		if (this.handler == null) {
			this.handler = HttpServerHandler.create(
					this.beanFactory.getBean(HttpTracing.class),
					new TraceWebFilter.HttpAdapter());
		}
		return this.handler;
	}

	Tracer tracer() {
		if (this.tracer == null) {
			this.tracer = this.beanFactory.getBean(HttpTracing.class).tracing().tracer();
		}
		return this.tracer;
	}

	TraceContext.Extractor<HttpHeaders> extractor() {
		if (this.extractor == null) {
			this.extractor = this.beanFactory.getBean(HttpTracing.class).tracing()
					.propagation().extractor(GETTER);
		}
		return this.extractor;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		if (tracer().currentSpan() != null) {
			// clear any previous trace
			tracer().withSpanInScope(null);
		}
		ServerHttpRequest request = exchange.getRequest();
		ServerHttpResponse response = exchange.getResponse();
		String uri = request.getPath().pathWithinApplication().value();

		boolean skip = (this.skipUriMatcher != null && this.skipUriMatcher.match(uri))
				|| "0".equals(request.getHeaders().getFirst("X-B3-Sampled"));
		if (log.isDebugEnabled()) {
			log.debug("Received a request to uri [" + uri
					+ "] that should not be sampled [" + skip + "]");
		}
		Span spanFromAttribute = getSpanFromAttribute(exchange);
		String name = HTTP_COMPONENT + ":" + uri;
		final String CONTEXT_ERROR = "sleuth.webfilter.context.error";
		AtomicReference<Span> spanRef = new AtomicReference<>();

		return chain.filter(exchange).compose(f -> f.then(Mono.subscriberContext())
				.onErrorResume(
						t -> Mono.subscriberContext().map(c -> c.put(CONTEXT_ERROR, t)))
				.flatMap(c -> {
					// reactivate
					// span
					// from
					// context
					Span span = spanFromContext(c);
					Mono<Void> continuation;
					Throwable t = null;
					if (c.hasKey(CONTEXT_ERROR)) {
						t = c.get(CONTEXT_ERROR);
						continuation = Mono.error(t);
					}
					else {
						continuation = Mono.empty();
						Object attribute = exchange.getAttribute(
								HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE);
						if (attribute instanceof HandlerMethod) {
							HandlerMethod handlerMethod = (HandlerMethod) attribute;
							addClassMethodTag(handlerMethod, span);
							addClassNameTag(handlerMethod, span);
						}

					}
					addResponseTagsForSpanWithoutParent(exchange, response, span);
					handler().handleSend(response, t, span);
					if (log.isDebugEnabled()) {
						log.debug("Handled send of " + span);
					}
					return continuation;
				}).doOnSubscribe((subscription -> {
					Span span = spanRef.get();
					if (span != null) {
						try (Tracer.SpanInScope spanInScope = tracer
								.withSpanInScope(span)) {
							// 一定要放在SpanInScope中，否则CurrentContext不正确。
							PropagationUtils.setOriginEnvIfNotExists(span.context(),
									EnvironmentUtils.getCurrentEnv());
							// any downstream code can see Tracer.currentSpan() or use
							// Tracer.currentSpanCustomizer()
							SpanUtils.tagPropagationInfos(span);
						}
					}

				})).subscriberContext(c -> {
					Span span;
					if (c.hasKey(Span.class)) {
						Span parent = c.get(Span.class);
						span = tracer().nextSpan(
								TraceContextOrSamplingFlags.create(parent.context()))
								.start();
						if (log.isDebugEnabled()) {
							log.debug("Found span in reactor context" + span);
						}
					}
					else {
						try {
							boolean hasTracingContextInHeaders = extractor().extract(
									request.getHeaders()) != TraceContextOrSamplingFlags.EMPTY;
							// if
							// there
							// was
							// a
							// received
							// span
							// then
							// we
							// must
							// not
							// change
							// the
							// sampling
							// decision
							if (skip && !hasTracingContextInHeaders) {
								span = unsampledSpan(name);
							}
							else {
								if (spanFromAttribute != null) {
									span = spanFromAttribute;
									if (log.isDebugEnabled()) {
										log.debug("Found span in attribute " + span);
									}
								}
								else {
									span = handler().handleReceive(extractor(),
											request.getHeaders(), request);
									if (log.isDebugEnabled()) {
										log.debug("Handled receive of span " + span);
									}
								}
							}
							exchange.getAttributes().put(TRACE_REQUEST_ATTR, span);
						}
						catch (Exception e) {
							log.error(
									"Exception occurred while trying to parse the request. "
											+ "Will fallback to manual span setting",
									e);
							if (skip) {
								span = unsampledSpan(name);
							}
							else {
								span = tracer().nextSpan().name(name).start();
								exchange.getAttributes().put(TRACE_SPAN_WITHOUT_PARENT,
										span);
								if (log.isDebugEnabled()) {
									log.debug("Created a new 'fallback' span " + span);
								}
							}
						}
					}

					SpanUtils.safeTag(span, SpanType.TAG_KEY, SpanType.HTTP);
					if (span != null && !span.isNoop()) {
						SpanUtils.safeTag(span, CommonTagKeys.SERVER_ENV,
								EnvironmentUtils.getCurrentEnv());

					}
					spanRef.set(span);
					tagServerHttpRequestHeaders(request, span);
					tagServerHttpRequestParams(request, span);
					return c.put(Span.class, span);
				}));
	}

	private static void tagServerHttpRequestParams(ServerHttpRequest request, Span span) {
		if (request == null || span == null || span.isNoop()) {
			return;
		}

		String requestUrl = request.getURI().toString();
		if (StringUtils.isNotBlank(requestUrl)) {
			Map<String, String> params = new UrlParameterManagerDto(requestUrl)
					.getEncodedParams();
			params.entrySet().stream().forEach(entry -> SpanUtils.safeTag(span,
					"p_" + entry.getKey(), entry.getValue()));

		}

	}

	private static void tagServerHttpRequestHeaders(ServerHttpRequest request,
			Span span) {
		if (request == null || span == null || span.isNoop()) {
			return;
		}
		HttpHeaders httpHeaders = request.getHeaders();
		httpHeaders.entrySet().stream().forEach(entry -> SpanUtils.safeTag(span,
				"h_" + entry.getKey(), printPretty(entry.getValue())));

	}

	private static String printPretty(List<String> values) {
		if (values != null && values.size() == 1) {
			return values.get(0);
		}
		return TraceJsonUtils.toJson(values);
	}

	private Span spanFromContext(Context c) {
		if (c.hasKey(Span.class)) {
			Span span = c.get(Span.class);
			if (log.isDebugEnabled()) {
				log.debug("Found span in context " + span);
			}
			return span;
		}
		Span span = defaultSpan();
		if (log.isDebugEnabled()) {
			log.debug("No span found in context. Creating a new one " + span);
		}
		return span;
	}

	private Span defaultSpan() {
		return tracer().nextSpan().start();
	}

	private void addResponseTagsForSpanWithoutParent(ServerWebExchange exchange,
			ServerHttpResponse response, Span span) {
		if (spanWithoutParent(exchange) && response.getStatusCode() != null
				&& span != null) {
			span.tag("statusCode", String.valueOf(response.getStatusCode().value()));
		}
	}

	private Span unsampledSpan(String name) {
		Span span = tracer()
				.nextSpan(TraceContextOrSamplingFlags.create(SamplingFlags.NOT_SAMPLED))
				.name(name).kind(Span.Kind.SERVER).start();
		if (log.isDebugEnabled()) {
			log.debug("Created a new unsampled span " + span);
		}
		return span;
	}

	private Span getSpanFromAttribute(ServerWebExchange exchange) {
		return exchange.getAttribute(TRACE_REQUEST_ATTR);
	}

	private boolean spanWithoutParent(ServerWebExchange exchange) {
		return exchange.getAttribute(TRACE_SPAN_WITHOUT_PARENT) != null;
	}

	private void addClassMethodTag(Object handler, Span span) {
		if (handler instanceof HandlerMethod) {
			String methodName = ((HandlerMethod) handler).getMethod().getName();
			span.tag("method", methodName);
			if (log.isDebugEnabled()) {
				log.debug("Adding a method tag with value [" + methodName + "] to a span "
						+ span);
			}
		}
	}

	private void addClassNameTag(Object handler, Span span) {
		String className;
		if (handler instanceof HandlerMethod) {
			className = ((HandlerMethod) handler).getBeanType().getSimpleName();
		}
		else {
			className = handler.getClass().getSimpleName();
		}
		if (log.isDebugEnabled()) {
			log.debug("Adding a class tag with value [" + className + "] to a span "
					+ span);
		}
		span.tag("class", className);
	}

	@Override
	public int getOrder() {
		return ORDER;
	}

	static final class HttpAdapter
			extends brave.http.HttpServerAdapter<ServerHttpRequest, ServerHttpResponse> {

		@Override
		public String method(ServerHttpRequest request) {
			return request.getMethodValue();
		}

		@Override
		public String url(ServerHttpRequest request) {
			return request.getURI().toString();
		}

		@Override
		public String requestHeader(ServerHttpRequest request, String name) {
			Object result = request.getHeaders().getFirst(name);
			return result != null ? result.toString() : null;
		}

		@Override
		public Integer statusCode(ServerHttpResponse response) {
			return response.getStatusCode() != null ? response.getStatusCode().value()
					: null;
		}
	}
}
