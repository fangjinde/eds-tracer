package com.netease.edu.eds.trace.instrument.http.httpclient;

import brave.Span;
import brave.Tracer;
import brave.Tracing;
import brave.propagation.Propagation;
import brave.propagation.TraceContext;
import com.netease.edu.eds.trace.constants.CommonTagKeys;
import com.netease.edu.eds.trace.core.Invoker;
import com.netease.edu.eds.trace.support.AbstractTraceAgentInstrumetation;
import com.netease.edu.eds.trace.utils.EnvironmentUtils;
import com.netease.edu.eds.trace.utils.PropagationUtils;
import com.netease.edu.eds.trace.utils.SpanUtils;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bind.annotation.*;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;
import org.apache.commons.lang.StringUtils;
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
public class HttpClientTraceInstrumentation extends AbstractTraceAgentInstrumetation {

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

			HttpRequest httpRequest = (HttpRequest) args[1];
			String httpMethod = httpRequest.getRequestLine().getMethod();
			String uri = httpRequest.getRequestLine().getUri();

			if (HttpRequestBypassSupport.byPassTrace(uri)) {
				return invoker.invoke(args);
			}

			Span span = tracer.nextSpan();
			span.kind(Span.Kind.CLIENT).start();

			span.name(httpMethod + ":" + uri);
			SpanUtils.safeTag(span, CommonTagKeys.CLIENT_ENV,
					EnvironmentUtils.getCurrentEnv());

			Object retObject = null;
			try (Tracer.SpanInScope spanInScope = tracer.withSpanInScope(span)) {

				// 一定要放在SpanInScope中，否则CurrentContext不正确。
				PropagationUtils.setOriginEnvIfNotExists(span.context(),
						EnvironmentUtils.getCurrentEnv());
				getInjector().inject(span.context(), httpRequest);
				SpanUtils.tagPropagationInfos(span);

				retObject = invoker.invoke(args);
				return retObject;
			}
			catch (Throwable e) {
				SpanUtils.tagError(span, e);
				SpanUtils.tagErrorMark(span);
				throw e;
			}
			finally {
				span.finish();
			}

		}



		private static final Propagation.Setter<HttpRequest, String> SETTER = new Propagation.Setter<HttpRequest, String>() {
			@Override
			public void put(HttpRequest carrier, String key, String value) {
				if (carrier != null && StringUtils.isNotBlank(key)) {
					carrier.addHeader(key, value);
				}
			}

			@Override
			public String toString() {
				return "HttpMessage::addHeader";
			}
		};

		private static TraceContext.Injector<HttpRequest> injector = null;

		private static TraceContext.Injector<HttpRequest> getInjector() {

			if (injector != null) {
				return injector;
			}

			synchronized (Interceptor.class) {
				if (injector == null) {
					Tracing tracing = Tracing.current();
					if (tracing != null) {
						injector = tracing.propagation().injector(SETTER);
					}
				}
			}
			return injector;

		}
	}
}
