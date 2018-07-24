package com.netease.edu.eds.shuffle.core;

import com.netease.edu.eds.trace.support.SpringBeanFactorySupport;

/**
 * @author hzfjd
 * @create 18/7/20
 **/
public class ShufflePropertiesSupport {

    public static String getStandardEnvName() {
        ShuffleProperties shuffleProperties = SpringBeanFactorySupport.getBean(ShuffleProperties.class);
        if (shuffleProperties == null) {
            return ShuffleProperties.STANDARD_ENV_NAME;
        }
        return shuffleProperties.getStandardEnvName();
    }
}
