package com.netease.edu.eds.trace.instrument.async;

import com.netease.edu.eds.trace.instrument.async.bootstrapclass.ThreadBootstrapPatchStub;
import com.netease.edu.eds.trace.spi.TraceAgentInstrumetation;
import com.netease.edu.eds.trace.support.AgentSupport;
import com.netease.edu.eds.trace.support.BootstrapDelegateListener;

import java.lang.instrument.Instrumentation;
import java.util.Map;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static net.bytebuddy.matcher.ElementMatchers.namedIgnoreCase;

/**
 * 该类不要通过ServiceLoader加载。因为不能再Thread加载之后做。
 * 
 * @author hzfjd
 * @create 18/7/16
 **/
public class ThreadInstrumentation implements TraceAgentInstrumetation {

    @Override
    public void premain(Map<String, String> props, Instrumentation inst) {
        AgentSupport.getBootstrapAgentBuilder(inst).type(namedIgnoreCase("java.lang.Thread")).transform((builder,
                                                                                                         typeDescription,
                                                                                                         classLoader,
                                                                                                         module) -> builder.method(namedIgnoreCase("start").and(isDeclaredBy(typeDescription))).intercept(AgentSupport.getInvokerMethodDelegationCustomer().to(ThreadBootstrapPatchStub.class))).with(BootstrapDelegateListener.newBootstrapAgentBuildLister(ThreadBootstrapPatchStub.class)).installOn(inst);

    }
}
