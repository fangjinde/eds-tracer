package com.netease.edu.eds.shuffle.instrument.lbclient;

import brave.Span;
import brave.Tracer;
import brave.Tracing;
import com.netease.edu.eds.shuffle.core.EnvironmentShuffleUtils;
import com.netease.edu.eds.shuffle.core.ShuffleConstants;
import com.netease.edu.eds.shuffle.dto.PrefixOrSuffixInfoDto;
import com.netease.edu.eds.shuffle.support.ShuffleEnvironmentInfoProcessUtils;
import com.netease.edu.eds.trace.core.Invoker;
import com.netease.edu.eds.trace.support.AbstractTraceAgentInstrumetation;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bind.annotation.*;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.utility.JavaModule;
import org.apache.commons.lang.StringUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * @author hzfjd
 * @create 19/2/15
 **/
public class RibbonLoadBalancerClientShuffleInstrumentation
		extends AbstractTraceAgentInstrumetation {

	@Override
	protected ElementMatcher.Junction defineTypeMatcher(Map<String, String> props) {
		return namedIgnoreCase(
				"org.springframework.cloud.netflix.ribbon.RibbonLoadBalancerClient");
	}

	@Override
	protected ElementMatcher.Junction defineMethodMatcher(Map<String, String> props,
			TypeDescription typeDescription, ClassLoader classLoader, JavaModule module) {
		// protected ILoadBalancer getLoadBalancer(String serviceId)
		// ElementMatcher.Junction getLoadBalancer1 = namedIgnoreCase("getLoadBalancer")
		// .and(isDeclaredBy(typeDescription)).and(takesArguments(1))
		// .and(isProtected());

		// public ServiceInstance choose(String serviceId)

		ElementMatcher.Junction choose1 = namedIgnoreCase("choose")
				.and(isDeclaredBy(typeDescription)).and(takesArguments(1))
				.and(isPublic());

		// public <T> T execute(String serviceId, LoadBalancerRequest<T> request) throws
		// IOException
		ElementMatcher.Junction execute2 = namedIgnoreCase("execute")
				.and(isDeclaredBy(typeDescription)).and(takesArguments(2))
				.and(isPublic());

		return choose1.or(execute2);
	}

	@Override
	protected Class defineInterceptorClass(Map<String, String> props) {
		return Interceptor.class;
	}

	public static class Interceptor {

		@RuntimeType
		public static Object intercept(@AllArguments Object[] args,
				@Morph Invoker invoker, @Origin Method method, @This Object proxy) {

			Tracer tracer = Tracing.currentTracer();
			if (tracer == null) {
				return invoker.invoke(args);
			}

			String serviceId = (String) args[0];
			if (StringUtils.isBlank(serviceId)) {
				return invoker.invoke(args);
			}

			Span curSpan = tracer.currentSpan();

			List<String> environmentsForPropagationSelection = EnvironmentShuffleUtils
					.getAllEnvironmentsForPropagationSelection(curSpan == null);

			String shuffledServiceId = getShuffledServiceId(
					environmentsForPropagationSelection, serviceId.toLowerCase());
			args[0] = shuffledServiceId.toUpperCase();

			return invoker.invoke(args);

		}

		/**
		 * 定位出原环境前后缀，并使用传播来源、当前环境、基准环境等中合适的环境进行替换。
		 * @param selectableEnvList
		 * @param serviceId
		 * @return
		 */
		public static String getShuffledServiceId(List<String> selectableEnvList,
				String serviceId) {

			List<String> allEnvList = new ArrayList<>(selectableEnvList);
			allEnvList.add(ShuffleConstants.LEGACY_STANDARD_ENV_NAME);

			for (String env : allEnvList) {
				PrefixOrSuffixInfoDto prefixOrSuffixInfoDto = ShuffleEnvironmentInfoProcessUtils
						.getPrefixOrSuffixInfo(serviceId, env);
				if (prefixOrSuffixInfoDto.isExist()) {
					for (String originOrStdEnv : selectableEnvList) {
						String shuffledServiceId = ShuffleEnvironmentInfoProcessUtils
								.getNameWithNewFixOrRemoveOldFix(serviceId, env,
										originOrStdEnv);
						if (StringUtils.isNotBlank(shuffledServiceId)) {
							return shuffledServiceId;
						}
					}
				}
			}

			return serviceId;
		}
	}
}
