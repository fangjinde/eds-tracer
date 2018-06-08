package com.netease.edu.eds.trace.utils;

/**
 * @author hzfjd
 * @create 18/6/8
 **/
public class ExceptionHandler {

    public static void throwExceptionAndMakeSureRuntime(Exception e) {
        if (e instanceof RuntimeException) {
            throw (RuntimeException) e;

        } else {
            throw new RuntimeException("wrap to RuntimeException", e);
        }
    }
}
