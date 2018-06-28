package com.netease.edu.eds.trace.boot;

import com.netease.edu.eds.trace.spi.TraceAgentInstrumetation;
import com.netease.edu.eds.trace.support.TraceInstrumentationHolder;

import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.logging.Level;

/**
 * @author hzfjd
 * @create 18/6/1
 **/
public class PluginLoader {

    private static volatile boolean inited = false;

    public static void load() {
        if (inited) {
            return;
        }
        synchronized (PluginLoader.class) {
            if (inited) {
                return;
            }

            innerAddTraceInstrumentations();
            inited = true;
        }
    }

    private static void addTraceInstrumentations() {

    }

    private static void innerAddTraceInstrumentations() {
        if (TraceInstrumentationHolder.getProps() == null || TraceInstrumentationHolder.getInstumentation() == null) {
            return;
        }
        TraceInstrumentationHolder.getLog().info("begin premain with agentArgs:"
                                                 + TraceInstrumentationHolder.getProps().get("agentArgs"));
        ServiceLoader<TraceAgentInstrumetation> serviceLoader = ServiceLoader.load(TraceAgentInstrumetation.class);

        Iterator<TraceAgentInstrumetation> iterator = serviceLoader.iterator();
        while (iterator.hasNext()) {
            TraceAgentInstrumetation traceAgentInstrumetation = iterator.next();
            TraceInstrumentationHolder.getLog().info("current instrumentation is :"
                                                     + traceAgentInstrumetation.getClass().getName());
            try {
                TraceInstrumentationHolder.getLog().info(traceAgentInstrumetation.getClass().getName()
                                                         + " beginning to instrumentation...");
                traceAgentInstrumetation.premain(TraceInstrumentationHolder.getProps(),
                                                 TraceInstrumentationHolder.getInstumentation());
                TraceInstrumentationHolder.getLog().info(traceAgentInstrumetation.getClass().getName()
                                                         + " 's instrumentation finished.");
            } catch (Exception e) {
                TraceInstrumentationHolder.getLog().log(Level.SEVERE, traceAgentInstrumetation.getClass().getName()
                                                                      + " 's instrumetation error:",
                                                        e);
            }
        }
        TraceInstrumentationHolder.getLog().info("end of premain with agentArgs:"
                                                 + TraceInstrumentationHolder.getProps().get("agentArgs"));
    }

}
