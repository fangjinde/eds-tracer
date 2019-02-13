package com.netease.edu.eds.trace.springboot2.instrument.http;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.gateway.filter.headers.HttpHeadersFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ServerWebExchange;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author hzfjd
 * @create 19/2/13
 **/
@ConfigurationProperties("spring.cloud.gateway.filter.remove-trace-headers")
public class TraceHttpHeadersFilter implements HttpHeadersFilter, Ordered {

	/**
	 * 128 or 64-bit trace ID lower-hex encoded into 32 or 16 characters (required)
	 */
	static final String TRACE_ID_NAME = "X-B3-TraceId";
	/**
	 * 64-bit span ID lower-hex encoded into 16 characters (required)
	 */
	static final String SPAN_ID_NAME = "X-B3-SpanId";
	/**
	 * 64-bit parent span ID lower-hex encoded into 16 characters (absent on root span)
	 */
	static final String PARENT_SPAN_ID_NAME = "X-B3-ParentSpanId";
	/**
	 * "1" means report this span to the tracing system, "0" means do not. (absent means
	 * defer the decision to the receiver of this header).
	 */
	static final String SAMPLED_NAME = "X-B3-Sampled";
	/**
	 * "1" implies sampled and is a request to override collection-tier sampling policy.
	 */
	static final String FLAGS_NAME = "X-B3-Flags";

	public static final Set<String> HEADERS_REMOVED_ON_REQUEST = new HashSet(
			Arrays.asList(TRACE_ID_NAME, SPAN_ID_NAME, PARENT_SPAN_ID_NAME, SAMPLED_NAME,
					FLAGS_NAME));

	public TraceHttpHeadersFilter() {
		this.headers = HEADERS_REMOVED_ON_REQUEST;
	}

	private Set<String> headers;

	public void setHeaders(Set<String> headers) {
		this.headers = headers;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	private int order = 2147483647;

	@Override
	public HttpHeaders filter(HttpHeaders input, ServerWebExchange exchange) {
		HttpHeaders filtered = new HttpHeaders();
		input.entrySet().stream().filter((entry) -> {
			return !this.headers.contains(entry.getKey());
		}).forEach((entry) -> {
			filtered.addAll((String) entry.getKey(), (List) entry.getValue());
		});
		return filtered;
	}

	@Override
	public boolean supports(Type type) {
		return Type.REQUEST.equals(type);
	}

	@Override
	public int getOrder() {
		return order;
	}
}
