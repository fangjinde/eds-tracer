package com.netease.edu.eds.trace.instrument.async;

import com.netease.edu.eds.trace.instrument.async.bootstrapclass.ThreadPoolExecutorInterceptorBootstrapStub;
import com.netease.edu.eds.trace.spi.TraceAgentInstrumetation;
import com.netease.edu.eds.trace.support.AgentSupport;
import com.netease.edu.eds.trace.support.BootstrapDelegateListener;
import net.bytebuddy.implementation.MethodDelegation;

import java.lang.instrument.Instrumentation;
import java.util.Map;

import static net.bytebuddy.matcher.ElementMatchers.namedIgnoreCase;

/**
 * @author hzfjd
 * @create 18/5/17
 **/
public class ThreadPoolExecutorInstrumentation implements TraceAgentInstrumetation {

    @Override
    public void premain(Map<String, String> props, Instrumentation inst) {
        AgentSupport.getBootstrapAgentBuilder(inst).type(namedIgnoreCase("java.util.concurrent.ThreadPoolExecutor")).transform((builder,
                                                                                                                                typeDescription,
                                                                                                                                classLoader,
                                                                                                                                module) -> builder.method(namedIgnoreCase("execute")).intercept(MethodDelegation.to(ThreadPoolExecutorInterceptorBootstrapStub.class))).with(BootstrapDelegateListener.newBootstrapAgentBuildLister(ThreadPoolExecutorInterceptorBootstrapStub.class)).installOn(inst);

    }

}
