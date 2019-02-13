package com.netease.edu.eds.shuffle.spi;

/**
 * @author hzfjd
 * @create 18/8/2
 **/
public interface EnvironmentDetector {

    boolean exist(String environmentName, String applicationName);
}
