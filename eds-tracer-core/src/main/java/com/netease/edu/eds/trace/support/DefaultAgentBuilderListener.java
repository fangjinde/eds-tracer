package com.netease.edu.eds.trace.support;/**
                                           * Created by hzfjd on 18/4/25.
                                           */

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.utility.JavaModule;

/**
 * @author hzfjd
 * @create 18/4/25
 */
public class DefaultAgentBuilderListener extends AgentBuilder.Listener.Adapter {

    private static AgentBuilder.Listener s_instance = new DefaultAgentBuilderListener();

    public static AgentBuilder.Listener getInstance() {
        return s_instance;
    }

    @Override
    public void onTransformation(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module,
                                 boolean loaded, DynamicType dynamicType) {
        TraceInstrumentationHolder.getLog().info(String.format("type: %s loaded by %s will be transformed.",
                                                               typeDescription.getTypeName(), classLoader));

    }

    @Override
    public void onError(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded,
                        Throwable throwable) {
        TraceInstrumentationHolder.getLog().error(String.format("type: %s loaded by %s can't be transformed cause by error:",
                                                                typeName, classLoader),
                                                  throwable);
    }

}
