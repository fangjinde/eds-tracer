package com.netease.edu.eds.trace.core;

/**
 * @author hzfjd
 */
public interface Invoker {

    /**
     * 方法调用
     * 
     * @param args
     * @return
     */
    Object invoke(Object[] args);
}
