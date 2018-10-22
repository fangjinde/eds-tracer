package com.netease.edu.eds.trace.support;

import com.netease.edu.eds.trace.spi.TraceAgentInstrumetation;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.utility.JavaModule;

import java.lang.instrument.Instrumentation;
import java.util.Map;

/**
 * @author hzfjd
 * @create 18/10/22
 **/
public abstract class AbstractTraceAgentInstrumetation implements TraceAgentInstrumetation {

    @Override
    public void premain(Map<String, String> props, Instrumentation inst) {

        if (bootstrapClass(props)) {

            AgentBuilder.Transformer transformer = (builder, typeDescription, classLoader, javaModule) -> {

                return builder.method(defineMethodMatcher(props, typeDescription, classLoader,
                                                          javaModule)).intercept(AgentSupport.getInvokerMethodDelegationCustomer().to(defineInterceptorClass(props)));
            };

            AgentSupport.getBootstrapAgentBuilder(inst).type(defineTypeMatcher(props)).transform(transformer).with(BootstrapDelegateListener.newBootstrapAgentBuildLister(defineInterceptorClass(props))).installOn(inst);
        } else {

            AgentBuilder.Transformer transformer = (builder, typeDescription, classLoader, javaModule) -> {

                return builder.method(defineMethodMatcher(props, typeDescription, classLoader,
                                                          javaModule)).intercept(AgentSupport.getInvokerMethodDelegationCustomer().to(defineInterceptorClass(props)));
            };

            new AgentBuilder.Default().type(defineTypeMatcher(props)).transform(transformer).with(DefaultAgentBuilderListener.getInstance()).installOn(inst);
        }

    }

    protected abstract ElementMatcher.Junction defineTypeMatcher(Map<String, String> props);

    protected abstract ElementMatcher.Junction defineMethodMatcher(Map<String, String> props,
                                                                   TypeDescription typeDescription,
                                                                   ClassLoader classLoader, JavaModule module);

    protected abstract Class defineInterceptorClass(Map<String, String> props);

    protected boolean bootstrapClass(Map<String, String> props) {
        return false;
    }
}
