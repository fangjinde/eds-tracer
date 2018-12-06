package com.netease.edu.eds.trace.boot.listener;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.ConfigurableEnvironment;

import com.netease.edu.eds.trace.boot.PluginLoader;

/**
 * SpringApplicationRunListener spring boot 1和2版本不兼容，弃用。
 * @author hzfjd
 * @create 18/5/22
 **/
@Deprecated
public class DeferedInstrumentationSpringApplicationRunListener implements SpringApplicationRunListener, PriorityOrdered {

    private final SpringApplication application;

    private final String[]          args;

    public DeferedInstrumentationSpringApplicationRunListener(SpringApplication application, String[] args) {
        this.application = application;
        this.args = args;
    }

    @Override
    public void starting() {
        PluginLoader.load();
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
