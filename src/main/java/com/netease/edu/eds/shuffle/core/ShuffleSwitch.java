package com.netease.edu.eds.shuffle.core;

import com.netease.edu.eds.trace.support.SpringBeanFactorySupport;

/**
 * @author hzfjd
 * @create 18/7/20
 **/
public class ShuffleSwitch {

    public static boolean isTurnOn() {
        ShuffleProperties shuffleProperties = SpringBeanFactorySupport.getBean(ShuffleProperties.class);
        if (shuffleProperties == null) {
            return false;
        }
        return shuffleProperties.isTurnOn();
    }

}
