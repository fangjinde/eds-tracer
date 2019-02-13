package com.netease.edu.eds.trace.utils;

/**
 * @author hzfjd
 * @create 18/6/28
 **/
public class ClassUtils {

    public static ClassLoader getClassLoader() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader != null) {
            return classLoader;
        }
        return ClassLoader.getSystemClassLoader();

    }
}
