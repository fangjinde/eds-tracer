package com.netease.edu.eds.trace.instrument.async;

import com.netease.edu.eds.trace.instrument.async.bootstrapclass.ThreadBootstrapPatchStub;
import com.netease.edu.eds.trace.spi.TraceAgentInstrumetation;
import com.netease.edu.eds.trace.support.AgentSupport;
import com.netease.edu.eds.trace.support.BootstrapDelegateListener;
import com.netease.edu.eds.trace.support.TraceInstrumentationHolder;
import net.bytebuddy.agent.builder.AgentBuilder;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.Map;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static net.bytebuddy.matcher.ElementMatchers.namedIgnoreCase;

/**
 * //太早加载的bootstrap类，premain开始前便已经加载。只能通过retransform进行增强，后期需要换成其他字节码增强工具。
 * 
 * @author hzfjd
 * @create 18/7/16
 **/
@Deprecated
public class ThreadInstrumentation implements TraceAgentInstrumetation {

    @Override
    public void premain(Map<String, String> props, Instrumentation inst) {
        AgentSupport.getBootstrapAgentBuilder(inst).with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION).type(namedIgnoreCase("java.lang.Thread")).transform((builder,
                                                                                                                                                                  typeDescription,
                                                                                                                                                                  classLoader,
                                                                                                                                                                  module) -> builder.method(namedIgnoreCase("start").and(isDeclaredBy(typeDescription))).intercept(AgentSupport.getInvokerMethodDelegationCustomer().to(ThreadBootstrapPatchStub.class))).with(BootstrapDelegateListener.newBootstrapAgentBuildLister(ThreadBootstrapPatchStub.class)).installOn(inst);

        try {
            inst.retransformClasses(Thread.class);
        } catch (UnmodifiableClassException e) {
            TraceInstrumentationHolder.getLog().error("ThreadInstrumentation.premain inst.retransformClasses Thread.class error",
                                                      e);
        }

    }
}
