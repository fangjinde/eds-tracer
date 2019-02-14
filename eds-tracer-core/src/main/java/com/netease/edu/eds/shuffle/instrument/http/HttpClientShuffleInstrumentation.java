package com.netease.edu.eds.shuffle.instrument.http;

import brave.Span;
import brave.Tracer;
import brave.Tracing;
import com.netease.edu.eds.trace.core.Invoker;
import com.netease.edu.eds.trace.instrument.http.httpclient.HttpRequestBypassSupport;
import com.netease.edu.eds.trace.support.AbstractTraceAgentInstrumetation;
import com.netease.edu.eds.trace.utils.SpanUtils;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bind.annotation.*;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Map;

import static net.bytebuddy.matcher.ElementMatchers.isOverriddenFrom;
import static net.bytebuddy.matcher.ElementMatchers.namedIgnoreCase;

/**
 * @author hzfjd
 * @create 19/2/14
 **/
public class HttpClientShuffleInstrumentation extends AbstractTraceAgentInstrumetation {

	private String CLOSEABLE_HTTP_CLIENT = "org.apache.http.impl.client.CloseableHttpClient";

	@Override
	protected ElementMatcher.Junction defineTypeMatcher(Map<String, String> props) {
		return ElementMatchers
				.hasSuperType(ElementMatchers.namedIgnoreCase(CLOSEABLE_HTTP_CLIENT));
	}

	@Override
	protected ElementMatcher.Junction defineMethodMatcher(Map<String, String> props,
			TypeDescription typeDescription, ClassLoader classLoader, JavaModule module) {
		// protected abstract CloseableHttpResponse doExecute(HttpHost target, HttpRequest
		// request,
		// HttpContext context) throws IOException, ClientProtocolException;

		ElementMatcher.Junction doExecute3 = namedIgnoreCase("doExecute")
				.and(isOverriddenFrom(namedIgnoreCase(CLOSEABLE_HTTP_CLIENT)))
				.and(ElementMatchers.takesArguments(3))
				.and(ElementMatchers.isProtected());
		return doExecute3;
	}

	@Override
	protected Class defineInterceptorClass(Map<String, String> props) {
		return Interceptor.class;
	}

	public static class Interceptor {

		private static final Logger log = LoggerFactory.getLogger(Interceptor.class);

		@RuntimeType
		public static Object intercept(@AllArguments Object[] args,
				@Morph Invoker invoker, @Origin Method method, @This Object proxy) {

			Tracer tracer = Tracing.currentTracer();
			if (tracer == null) {
				return invoker.invoke(args);
			}

			HttpHost httpHost = (HttpHost) args[0];
			String hostName = httpHost.getHostName();
			HttpRequest httpRequest = (HttpRequest) args[1];
			String uri = httpRequest.getRequestLine().getUri();

			if (HttpRequestBypassSupport.byPassTrace(uri)) {
				return invoker.invoke(args);
			}

			if ("echo.fjd.com".equalsIgnoreCase(hostName)) {
				// http://127.0.0.1:9999
				HttpHost newHttpHost = new HttpHost("127.0.0.1", 9999,
						httpHost.getSchemeName());
				args[0] = newHttpHost;
				httpRequest.addHeader("Host", hostName);

				Span span = tracer.currentSpan();
				SpanUtils.safeTag(span, "HttpClientOriginHost", hostName);
				SpanUtils.safeTag(span, "HttpClientRewriteTo", "127.0.0.1:9999");
			}

			return invoker.invoke(args);

		}
	}
}
