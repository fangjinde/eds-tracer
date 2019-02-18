package com.netease.edu.eds.trace.demo.component;

import com.netease.edu.web.health.HealthCheckProcessor;

/**
 * @author hzfjd
 * @create 19/2/18
 **/
public class DefaultHealthCheckProcessor implements HealthCheckProcessor {

    @Override
    public boolean checkHealth() {
        return true;
    }
}
