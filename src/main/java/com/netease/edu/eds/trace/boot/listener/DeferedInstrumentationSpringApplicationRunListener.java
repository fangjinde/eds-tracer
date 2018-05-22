package com.netease.edu.eds.trace.boot.listener;

import com.netease.edu.eds.trace.spi.TraceAgentInstrumetation;
import com.netease.edu.eds.trace.support.LoggerFactory;
import com.netease.edu.eds.trace.support.TraceInstrumentationHolder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author hzfjd
 * @create 18/5/22
 **/
public class DeferedInstrumentationSpringApplicationRunListener implements SpringApplicationRunListener, PriorityOrdered {

    private static final Logger     logger = LoggerFactory.getLogger(DeferedInstrumentationSpringApplicationRunListener.class);

    private final SpringApplication application;

    private final String[]          args;

    public DeferedInstrumentationSpringApplicationRunListener(SpringApplication application, String[] args) {
        this.application = application;
        this.args = args;
    }

    private static volatile boolean inited = false;

    private static void addTraceInstrumentations() {
        if (inited) {
            return;
        }
        synchronized (DeferedInstrumentationSpringApplicationRunListener.class) {
            if (inited) {
                return;
            }

            innerAddTraceInstrumentations();
            inited = true;
        }
    }

    private static void innerAddTraceInstrumentations() {
        if (TraceInstrumentationHolder.getProps() == null || TraceInstrumentationHolder.getInstumentation() == null) {
            return;
        }
        logger.info("begin premain with agentArgs:" + TraceInstrumentationHolder.getProps().get("agentArgs"));
        ServiceLoader<TraceAgentInstrumetation> serviceLoader = ServiceLoader.load(TraceAgentInstrumetation.class);

        Iterator<TraceAgentInstrumetation> iterator = serviceLoader.iterator();
        while (iterator.hasNext()) {
            TraceAgentInstrumetation traceAgentInstrumetation = iterator.next();
            logger.info("current instrumentation is :" + traceAgentInstrumetation.getClass().getName());
            try {
                logger.info(traceAgentInstrumetation.getClass().getName() + " beginning to instrumentation...");
                traceAgentInstrumetation.premain(TraceInstrumentationHolder.getProps(),
                                                 TraceInstrumentationHolder.getInstumentation());
                logger.info(traceAgentInstrumetation.getClass().getName() + " 's instrumentation finished.");
            } catch (Exception e) {
                logger.log(Level.SEVERE, traceAgentInstrumetation.getClass().getName() + " 's instrumetation error:",
                           e);
            }
        }
        logger.info("end of premain with agentArgs:" + TraceInstrumentationHolder.getProps().get("agentArgs"));
    }

    @Override
    public void starting() {
        addTraceInstrumentations();
    }

    @Override
    public void environmentPrepared(ConfigurableEnvironment environment) {

    }

    @Override
    public void contextPrepared(ConfigurableApplicationContext context) {

    }

    @Override
    public void contextLoaded(ConfigurableApplicationContext context) {

    }

    @Override
    public void finished(ConfigurableApplicationContext context, Throwable exception) {

    }

    @Override
    public int getOrder() {
        return Integer.MAX_VALUE;
    }
}
