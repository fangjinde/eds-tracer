package com.netease.edu.eds.trace.utils;

/**
 * @author hzfjd
 * @create 18/6/8
 **/
public class ExceptionHandler {

    public static void throwExceptionAndMakeSureRuntime(Exception e) throws RuntimeException {
        throw wrapToRuntimeException(e);
    }

    public static RuntimeException wrapToRuntimeException(Exception e) {
        if (e instanceof RuntimeException) {
            return (RuntimeException) e;

        } else {
            return new RuntimeException("wrap to RuntimeException", e);
        }
    }
}
