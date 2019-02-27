package com.netease.edu.eds.shuffle.instrument.http;

import brave.Span;
import brave.Tracer;
import brave.Tracing;
import com.netease.edu.eds.shuffle.core.ShufflePropertiesSupport;
import com.netease.edu.eds.shuffle.core.ShuffleSwitch;
import com.netease.edu.eds.trace.core.Invoker;
import com.netease.edu.eds.trace.instrument.http.httpclient.HttpRequestBypassSupport;
import com.netease.edu.eds.trace.support.AbstractTraceAgentInstrumetation;
import com.netease.edu.eds.trace.support.SpringBeanFactorySupport;
import com.netease.edu.eds.trace.utils.PropagationUtils;
import com.netease.edu.eds.trace.utils.SpanUtils;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bind.annotation.*;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

			if (!ShuffleSwitch.isTurnOn()) {
				return invoker.invoke(args);
			}

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

			Map<String, String> httpClientHostRewriteToMap = ShufflePropertiesSupport
					.getHttpClientHostRewriteTo();

			for (Map.Entry<String, String> entry : httpClientHostRewriteToMap
					.entrySet()) {
				if (Pattern.compile(entry.getKey()).matcher(hostName).find()) {

					ServiceInstance serviceInstance = getServiceInstanceByRoutingToUri(
							entry.getValue());

					if (serviceInstance == null) {
						continue;
					}

					String scheme = (serviceInstance.isSecure()) ? "https" : "http";
					HttpHost newHttpHost = new HttpHost(serviceInstance.getHost(),
							serviceInstance.getPort(), scheme);
					args[0] = newHttpHost;
					httpRequest.addHeader("Host", hostName);

					Span span = tracer.currentSpan();
					SpanUtils.safeTag(span, "HttpClientOriginHost", hostName);
					SpanUtils.safeTag(span, "HttpClientRewriteTo",
							serviceInstance.getUri().toString());
				}
			}

			return invoker.invoke(args);

		}

		static final String GW_SCHEMA = "gw";
		static final String GW_ORIGINAL_ENV = "original";

		static final Pattern pattern = Pattern.compile("^gw://([\\w\\.-]+)\\.([\\w-]+)$");

		// public static void main(String[] args) {
		// printMatch("gw://edu-sc-gateway.original");
		// printMatch("gw://eds-sc-gateway.edu-std");
		// }
		//
		// private static void printMatch(String url) {
		// Matcher matcher = pattern.matcher(url);
		// if (matcher.find()) {
		// System.out.println(url + " is matched. ");
		// System.out.println(matcher.group(1));
		// System.out.println(matcher.group(2));
		// }
		// else {
		// System.out.println(url + " not matched!");
		// }
		// }

		private static ServiceInstance getServiceInstanceByRoutingToUri(
				String routingToUrl) {

			if (StringUtils.isBlank(routingToUrl)) {
				return null;
			}

			Matcher matcher = pattern.matcher(routingToUrl);

			if (!matcher.find()) {
				return null;
			}

			DiscoveryClient discoveryClient = SpringBeanFactorySupport
					.getBean(DiscoveryClient.class);
			if (discoveryClient == null) {
				return null;
			}

			String serviceId = null;
			String serviceName = matcher.group(1);
			String env = matcher.group(2);

			if (GW_ORIGINAL_ENV.equalsIgnoreCase(env)) {
				serviceId = serviceName + "." + PropagationUtils.getOriginEnv();
			}
			else {
				serviceId = serviceName + "." + env;
			}

			List<ServiceInstance> serviceInstances = discoveryClient
					.getInstances(serviceId.toUpperCase());

			if (CollectionUtils.isEmpty(serviceInstances)) {
				return null;
			}

			if (serviceInstances.size() == 1) {
				return serviceInstances.get(0);
			}

			int randomIndex = new Random().nextInt(serviceInstances.size());
			return serviceInstances.get(randomIndex);
		}

	}
}
