package com.netease.edu.eds.trace.boot;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;

/**
 *
 * @author hzfjd
 * @create 18/6/1
 **/
public class PluginLoaderApplicationContextInitializer implements ApplicationContextInitializer, PriorityOrdered {

    public PluginLoaderApplicationContextInitializer() {
        PluginLoader.load();
    }

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {

    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
