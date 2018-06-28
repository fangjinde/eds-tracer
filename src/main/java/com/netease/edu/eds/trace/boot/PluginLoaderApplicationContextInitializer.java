package com.netease.edu.eds.trace.boot;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;

/**
 * 改为Bootstrap加载，已获得最及时的织入。
 * 
 * @author hzfjd
 * @create 18/6/1
 **/
@Deprecated
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
