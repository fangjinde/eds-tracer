package com.netease.edu.eds.trace.boot;

import com.netease.edu.eds.trace.instrument.async.ThreadInstrumentation;
import com.netease.edu.eds.trace.spi.TraceAgentInstrumetation;
import com.netease.edu.eds.trace.support.TraceInstrumentationHolder;

import java.lang.instrument.Instrumentation;
import java.util.Map;

/**
 * @author hzfjd
 * @create 18/6/28
 **/
public class SystemBootstrap {

    private static TraceAgentInstrumetation traceAgentInstrumetation = new ThreadInstrumentation();

    public static void start(Instrumentation instumentation, Map<String, String> props) {
        TraceInstrumentationHolder.getLog().info(" SystemBootstrap.start [[ begin all ]] with agentArgs:"
                                                 + TraceInstrumentationHolder.getProps().get("agentArgs"));
        try {
            TraceInstrumentationHolder.getLog().info(" SystemBootstrap.start [[ beginning ]] to instrument: [[ "
                                                     + traceAgentInstrumetation.getClass().getName() + " ]].");
            traceAgentInstrumetation.premain(TraceInstrumentationHolder.getProps(),
                                             TraceInstrumentationHolder.getInstumentation());
            TraceInstrumentationHolder.getLog().info(" SystemBootstrap.start [[ finished ]] to instrument [[ "
                                                     + traceAgentInstrumetation.getClass().getName() + " ]]. ");
        } catch (Exception e) {
            TraceInstrumentationHolder.getLog().error(" SystemBootstrap.start [[ fail ]] to instrument [[ "
                                                      + traceAgentInstrumetation.getClass().getName() + " ]]. ", e);
        }

        TraceInstrumentationHolder.getLog().info(" SystemBootstrap.start [[ end all ]] with agentArgs:"
                                                 + TraceInstrumentationHolder.getProps().get("agentArgs"));
    }

}
