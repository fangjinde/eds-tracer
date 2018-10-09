package com.netease.edu.eds.shuffle.dubbo;

import brave.Tracer;
import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.registry.integration.RegistryDirectory;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.RpcInvocation;
import com.alibaba.dubbo.rpc.cluster.Router;
import com.alibaba.dubbo.rpc.protocol.InvokerWrapper;
import com.netease.edu.eds.shuffle.core.EnvironmentShuffleUtils;
import com.netease.edu.eds.shuffle.core.ShuffleSwitch;
import com.netease.edu.eds.trace.core.Invoker;
import com.netease.edu.eds.trace.spi.TraceAgentInstrumetation;
import com.netease.edu.eds.trace.support.AgentSupport;
import com.netease.edu.eds.trace.support.DefaultAgentBuilderListener;
import com.netease.edu.eds.trace.support.SpringBeanFactorySupport;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bind.annotation.*;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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

        ElementMatcher.Junction classMatcher = namedIgnoreCase("com.alibaba.dubbo.registry.integration.RegistryDirectory");

        new AgentBuilder.Default().type(classMatcher).transform((builder, typeDescription, classloader,
                                                                 javaModule) -> builder.method(getMethodMatcher(typeDescription)).intercept(AgentSupport.getInvokerMethodDelegationCustomer().to(Interceptor.class))).with(DefaultAgentBuilderListener.getInstance()).installOn(inst);

    }

    private static ElementMatcher.Junction getMethodMatcher(TypeDescription typeDescription) {
        // public List<Invoker<T>> doList(Invocation invocation)
        ElementMatcher.Junction methodMatcher1 = isDeclaredBy(typeDescription).and(namedIgnoreCase("doList")).and(takesArguments(1));

        // private List<Invoker<T>> route(List<Invoker<T>> invokers, String method)
        ElementMatcher.Junction methodMatcher2 = isDeclaredBy(typeDescription).and(namedIgnoreCase("route")).and(takesArguments(2));
        return methodMatcher1.or(methodMatcher2);
    }

    public static class Interceptor {

        @RuntimeType
        public static Object intercept(@AllArguments Object[] args, @Morph Invoker invoker, @Origin Method method,
                                       @This Object proxy) {

            // do for "getMessageListener" method
            if (method.getName().equals("route")) {
                return route(args, invoker, method, proxy);
            }
            return doList(args, invoker, method, proxy);
        }

        public static <T> Object route(Object[] args, Invoker invoker, Method method, Object proxy) {

            if (!ShuffleSwitch.isTurnOn()) {
                return invoker.invoke(args);
            }

            Tracer tracer = SpringBeanFactorySupport.getBean(Tracer.class);
            if (tracer == null || tracer.currentSpan() == null) {
                return invoker.invoke(args);
            }

            List<com.alibaba.dubbo.rpc.Invoker<T>> invokerList = (List<com.alibaba.dubbo.rpc.Invoker<T>>) args[0];
            String methodName = (String) args[1];

            Invocation invocation = new RpcInvocation(methodName, new Class<?>[0], new Object[0]);
            RegistryDirectory registryDirectory = (RegistryDirectory) proxy;
            List<Router> routers = registryDirectory.getRouters();
            if (routers != null) {
                for (Router router : routers) {
                    if (router.getUrl() != null && !router.getUrl().getParameter(Constants.RUNTIME_KEY, true)) {

                        // 只有路由url的version版本和consumer的version版本一致，路由规则才生效。因为原生的invoker
                        // 列表已经按照version分组。而我这里将所有的version都放到一起，因此需要在路由匹配前，再重新按照version进行分组。
                        // 否则，会导致对A version的路由规则，对所有的消费者版本都生效。
                        // 那么，还有个问题，对于服务提供者的版本呢？目前暂不处理。
                        String routeUrlVersion = router.getUrl().getParameter(Constants.VERSION_KEY);
                        URL consumerUrl = registryDirectory.getConsumerUrl();
                        String consumerUrlVersion = consumerUrl.getParameter(Constants.VERSION_KEY);
                        if (Constants.ANY_VALUE.equals(routeUrlVersion)
                            || (routeUrlVersion != null && routeUrlVersion.equals(consumerUrlVersion))) {
                            invokerList = router.route(invokerList, consumerUrl, invocation);
                        }

                    }
                }
            }
            return invokerList;
        }

        public static Object doList(Object[] args, Invoker invoker, Method method, Object proxy) {

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
            List<Object> dubboInvokerList = new ArrayList<>();
            for (Object dubboInvoker : (List<Object>) result) {
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
                            envInvokersSelectorMap.put(envSelected, invokerWrapperList);
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
