package com.netease.edu.eds.shuffle.core;

/**
 * @author hzfjd
 * @create 18/7/20
 **/
public class ShuffleSwitch {

    public static boolean isTurnOn() {
        return ShufflePropertiesSupport.turnOn();
    }

}
