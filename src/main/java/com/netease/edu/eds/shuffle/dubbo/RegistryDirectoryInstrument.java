package com.netease.edu.eds.shuffle.dubbo;

import brave.Tracer;
import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.protocol.InvokerWrapper;
import com.netease.edu.eds.shuffle.core.EnvironmentShuffleUtils;
import com.netease.edu.eds.shuffle.core.ShuffleSwitch;
import com.netease.edu.eds.trace.core.Invoker;
import com.netease.edu.eds.trace.spi.TraceAgentInstrumetation;
import com.netease.edu.eds.trace.support.AgentSupport;
import com.netease.edu.eds.trace.support.DefaultAgentBuilderListener;
import com.netease.edu.eds.trace.support.SpringBeanFactorySupport;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Morph;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.util.*;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * @author hzfjd
 * @create 18/7/20
 **/
public class RegistryDirectoryInstrument implements TraceAgentInstrumetation {

    private static final Logger logger = LoggerFactory.getLogger(RegistryDirectoryInstrument.class);

    @Override
    public void premain(Map<String, String> props, Instrumentation inst) {
        new AgentBuilder.Default().type(namedIgnoreCase("com.alibaba.dubbo.registry.integration.RegistryDirectory")).transform((builder,
                                                                                                                                typeDescription,
                                                                                                                                classloader,
                                                                                                                                javaModule) -> builder.method(isDeclaredBy(typeDescription).and(namedIgnoreCase("doList")).and(takesArguments(1))).intercept(AgentSupport.getInvokerMethodDelegationCustomer().to(Interceptor.class))).with(DefaultAgentBuilderListener.getInstance()).installOn(inst);

    }

    public static class Interceptor {

        @RuntimeType
        public static Object intercept(@AllArguments Object[] args, @Morph Invoker invoker) {
            if (!ShuffleSwitch.isTurnOn()) {
                return invoker.invoke(args);
            }

            Tracer tracer = SpringBeanFactorySupport.getBean(Tracer.class);
            if (tracer == null || tracer.currentSpan() == null) {
                return invoker.invoke(args);
            }

            Object result = invoker.invoke(args);
            // 检查返回值，安全防御
            if (!(result instanceof List)) {
                return result;
            }

            // dubbo和trace的Invoker别混了。
            // 原目录接口返回的List是Unmodifiable的，重新生成一个可修改的List
            List<Object> dubboInvokerList =new ArrayList<>();
            for ( Object dubboInvoker:(List<Object>) result){
                dubboInvokerList.add(dubboInvoker);
            }
            if (CollectionUtils.isEmpty(dubboInvokerList)) {
                return dubboInvokerList;
            }

            List<String> environmentsForPropagationSelection = EnvironmentShuffleUtils.getEnvironmentsForPropagationSelection();
            // 可供选择的环境不存在？
            if (CollectionUtils.isEmpty(environmentsForPropagationSelection)) {
                dubboInvokerList.clear();
                return dubboInvokerList;
            }

            Map<String, List<InvokerWrapper>> envInvokersSelectorMap = getEnvInvokersSelectorMap(environmentsForPropagationSelection,
                                                                                                 dubboInvokerList);
            // 所有环境都没有符合条件的invokers，则清空
            if (MapUtils.isEmpty(envInvokersSelectorMap)) {
                dubboInvokerList.clear();
                return dubboInvokerList;
            }

            // 取存在invoker并且优先级最高的环境所对应那一组Invokers
            List<InvokerWrapper> selectedInvokerWrapperList = null;
            for (String envSelected : environmentsForPropagationSelection) {
                selectedInvokerWrapperList = envInvokersSelectorMap.get(envSelected);
                if (CollectionUtils.isNotEmpty(selectedInvokerWrapperList)) {
                    break;
                }
            }

            // 安全防御。内部实现上，此时不应该再为空
            if (CollectionUtils.isEmpty(selectedInvokerWrapperList)) {
                dubboInvokerList.clear();
                return dubboInvokerList;
            }

            // 删除非选中的其他所有invoker
            Iterator<Object> dubboInvokerIterator = dubboInvokerList.iterator();
            while (dubboInvokerIterator.hasNext()) {
                Object curDubboInvoker = dubboInvokerIterator.next();
                if (!selectedInvokerWrapperList.contains(curDubboInvoker)) {
                    dubboInvokerIterator.remove();
                }
            }
            return dubboInvokerList;

        }

        /**
         * 按照环境优先顺序对invoker进行分组
         * 
         * @param environmentsForPropagationSelection
         * @param dubboInvokerList
         * @return
         */
        private static Map<String, List<InvokerWrapper>> getEnvInvokersSelectorMap(List<String> environmentsForPropagationSelection,
                                                                                   List<Object> dubboInvokerList) {
            Map<String, List<InvokerWrapper>> envInvokersSelectorMap = new HashMap<>(6);
            for (Object dubboInvoker : dubboInvokerList) {
                if (!(dubboInvoker instanceof InvokerWrapper)) {
                    continue;
                }

                InvokerWrapper dubboInvokerWrapper = (InvokerWrapper) dubboInvoker;
                String providerVersion = getProviderVersion(dubboInvokerWrapper);

                if (StringUtils.isBlank(providerVersion)) {
                    continue;

                }
                for (String envSelected : environmentsForPropagationSelection) {
                    if (providerVersion.endsWith(envSelected)) {
                        List<InvokerWrapper> invokerWrapperList = envInvokersSelectorMap.get(envSelected);
                        if (invokerWrapperList == null) {
                            invokerWrapperList = new ArrayList<>();
                            envInvokersSelectorMap.put(envSelected,invokerWrapperList);
                        }
                        invokerWrapperList.add(dubboInvokerWrapper);
                        // 不可能也不允许同时被多个环境选中。因此命中后，跳过后续环境检查。
                        break;
                    }
                }
            }
            return envInvokersSelectorMap;
        }

        /**
         * InvokerDelegete为私有类，暂时通过反射获取。下面的反射实现其实有性能优化空间。只在测试环境和预发布使用，暂不做优化。
         * 
         * @param dubboInvokerWrapper
         * @return
         */
        private static String getProviderVersion(InvokerWrapper dubboInvokerWrapper) {
            try {
                Field providerUrlField = dubboInvokerWrapper.getClass().getDeclaredField("providerUrl");
                ReflectionUtils.makeAccessible(providerUrlField);
                URL providerUrl = (URL) providerUrlField.get(dubboInvokerWrapper);
                if (providerUrl != null) {
                    return providerUrl.getParameter(Constants.VERSION_KEY);
                }
            } catch (Exception e) {
                logger.error("getProviderVersion error, with dubboInvokerWrapper:" + dubboInvokerWrapper.toString(), e);

            }
            return null;
        }

    }
}
