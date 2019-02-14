package com.netease.edu.eds.shuffle.dubbo;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.common.utils.UrlUtils;
import com.netease.edu.eds.shuffle.core.EnvironmentShuffleUtils;
import com.netease.edu.eds.shuffle.core.ShuffleConstants;
import com.netease.edu.eds.shuffle.core.ShuffleSwitch;
import com.netease.edu.eds.shuffle.dto.PrefixOrSuffixInfoDto;
import com.netease.edu.eds.shuffle.support.ShuffleEnvironmentInfoProcessUtils;
import com.netease.edu.eds.trace.core.Invoker;
import com.netease.edu.eds.trace.spi.TraceAgentInstrumetation;
import com.netease.edu.eds.trace.support.AgentSupport;
import com.netease.edu.eds.trace.support.DefaultAgentBuilderListener;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Morph;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;

import java.lang.instrument.Instrumentation;
import java.util.*;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * @author hzfjd
 * @create 18/7/20
 **/
public class DubboUrlUtilsInstrument implements TraceAgentInstrumetation {

    @Override
    public void premain(Map<String, String> props, Instrumentation inst) {
        new AgentBuilder.Default().type(namedIgnoreCase("com.alibaba.dubbo.common.utils.UrlUtils")).transform((builder,
                                                                                                               typeDescription,
                                                                                                               classloader,
                                                                                                               javaModule) -> builder.method(isDeclaredBy(typeDescription).and(namedIgnoreCase("isMatch")).and(takesArguments(2))).intercept(AgentSupport.getInvokerMethodDelegationCustomer().to(Interceptor.class))).with(DefaultAgentBuilderListener.getInstance()).installOn(inst);
    }

    public static class Interceptor {

        @RuntimeType
        public static Object intercept(@AllArguments Object[] args, @Morph Invoker invoker) {

            if (!ShuffleSwitch.isTurnOn()) {
                return invoker.invoke(args);
            }

            return isMatch((URL) args[0], (URL) args[1]);

        }

        /**
         * 对原方法进行改造，去除version匹配
         *
         * @param consumerUrl
         * @param providerUrl
         * @return
         */
        private static boolean isMatch(URL consumerUrl, URL providerUrl) {
            String consumerInterface = consumerUrl.getServiceInterface();
            String providerInterface = providerUrl.getServiceInterface();
            if (!(Constants.ANY_VALUE.equals(consumerInterface)
                  || StringUtils.isEquals(consumerInterface, providerInterface)))
                return false;

            // 注意，在方法体内部的类方法调用，不会触发类的预加载。因此此处调用是安全的。
            if (!UrlUtils.isMatchCategory(providerUrl.getParameter(Constants.CATEGORY_KEY, Constants.DEFAULT_CATEGORY),
                                          consumerUrl.getParameter(Constants.CATEGORY_KEY,
                                                                   Constants.DEFAULT_CATEGORY))) {
                return false;
            }
            if (!providerUrl.getParameter(Constants.ENABLED_KEY, true)
                && !Constants.ANY_VALUE.equals(consumerUrl.getParameter(Constants.ENABLED_KEY))) {
                return false;
            }

            String consumerGroup = consumerUrl.getParameter(Constants.GROUP_KEY);
            String consumerClassifier = consumerUrl.getParameter(Constants.CLASSIFIER_KEY, Constants.ANY_VALUE);

            String providerGroup = providerUrl.getParameter(Constants.GROUP_KEY);
            String providerClassifier = providerUrl.getParameter(Constants.CLASSIFIER_KEY, Constants.ANY_VALUE);
            return (Constants.ANY_VALUE.equals(consumerGroup) || StringUtils.isEquals(consumerGroup, providerGroup)
                    || StringUtils.isContains(consumerGroup, providerGroup))

                   && (consumerClassifier == null || Constants.ANY_VALUE.equals(consumerClassifier)
                       || StringUtils.isEquals(consumerClassifier, providerClassifier));
        }

        /**
         * 如果consumerVersion中有包含xxx-std,xxx-edu-std,xxx-(来源环境后缀)等环境前后缀，则匹配相同名称为xxx-edu-std,xxx-(来源环境后缀)的providerVersion。否则匹配搜有的providerVersion（为了兼容有些历史遗留场景，开发使用了指定的某个测试环境进行匹配）。
         * 
         * @param consumerVersion
         * @param providerVersion
         * @return
         */
        @Deprecated
        private static boolean hasConsumerVersionSuitableProviderVersion(String consumerVersion,
                                                                         String providerVersion) {

            if (consumerVersion == null && providerVersion == null) {
                return true;
            }
            if (consumerVersion == null || providerVersion == null) {
                return false;
            }

            List<String> originAndStdEnvList = EnvironmentShuffleUtils.getOriginAndStdEnvironments();

            List<String> allEnvList = new ArrayList<>(originAndStdEnvList);
            allEnvList.add(ShuffleConstants.LEGACY_STANDARD_ENV_NAME);

            Set<String> suitableProviderVersionSet = new TreeSet<>();
            for (String env : allEnvList) {
                PrefixOrSuffixInfoDto prefixOrSuffixInfoDto = ShuffleEnvironmentInfoProcessUtils.getPrefixOrSuffixInfo(consumerVersion,
                                                                                                                       env);
                if (prefixOrSuffixInfoDto.isExist()) {
                    for (String originOrStdEnv : originAndStdEnvList) {
                        String suitableProviderVersion = ShuffleEnvironmentInfoProcessUtils.getNameWithNewFixOrRemoveOldFix(consumerVersion,
                                                                                                                            env,
                                                                                                                            originOrStdEnv);
                        suitableProviderVersionSet.add(suitableProviderVersion);

                    }

                }
            }

            if (!suitableProviderVersionSet.isEmpty()) {
                return suitableProviderVersionSet.contains(providerVersion);
            }

            return true;
        }

    }
}
