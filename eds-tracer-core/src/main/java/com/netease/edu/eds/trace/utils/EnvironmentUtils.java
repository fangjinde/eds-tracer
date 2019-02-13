package com.netease.edu.eds.trace.utils;

import com.netease.edu.eds.trace.support.SpringBeanFactorySupport;
import org.springframework.core.env.Environment;

/**
 * @author hzfjd
 * @create 18/10/15
 **/
public class EnvironmentUtils {

    /**
     * 获取当前环境
     *
     * @return
     */
    public static String getCurrentEnv() {
        Environment environment = SpringBeanFactorySupport.getBean(Environment.class);
        if (environment == null) {
            return null;
        }
        return environment.getProperty("spring.profiles.active");
    }

    /**
     * 获取当前应用名字
     *
     * @return
     */
    public static String getCurrentApplicationName() {
        Environment environment = SpringBeanFactorySupport.getBean(Environment.class);
        if (environment == null) {
            return null;
        }
        return environment.getProperty("spring.application.name");
    }

}
