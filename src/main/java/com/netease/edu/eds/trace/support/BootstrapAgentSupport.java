package com.netease.edu.eds.trace.support;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;

import java.io.File;
import java.lang.instrument.Instrumentation;

import static net.bytebuddy.matcher.ElementMatchers.isSynthetic;
import static net.bytebuddy.matcher.ElementMatchers.nameStartsWith;

/**
 * @author hzfjd
 * @create 18/7/2
 **/
public class BootstrapAgentSupport {

    public static AgentBuilder getBootstrapAgentBuilder(Instrumentation inst) {
        return new AgentBuilder.Default().enableBootstrapInjection(inst,
                                                                   new File(TraceInstrumentationHolder.getBootstrapLibPath())).ignore(new AgentBuilder.RawMatcher.ForElementMatchers(nameStartsWith("net.bytebuddy.").or(nameStartsWith("sun.reflect.")).<TypeDescription> or(isSynthetic())));
    }
}
