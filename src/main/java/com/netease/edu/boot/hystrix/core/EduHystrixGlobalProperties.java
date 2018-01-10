package com.netease.edu.boot.hystrix.core;/**
 * Created by hzfjd on 17/12/24.
 */

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author hzfjd
 * @create 17/12/24
 */
@ConfigurationProperties(prefix = "edu_hystrix")
public class EduHystrixGlobalProperties {

    public boolean isIsolatedByOriginEnable() {
        return isolatedByOriginEnable;
    }

    public void setIsolatedByOriginEnable(boolean isolatedByOriginEnable) {
        this.isolatedByOriginEnable = isolatedByOriginEnable;
    }
    private boolean isolatedByOriginEnable=false;

}
