package com.netease.edu.boot.hystrix.core;

/**
 * Created by hzfjd on 18/1/10.
 */
public interface ExceptionResultResolver<E extends Throwable,R> {
    boolean hasException(R result);
    E getException(R result);
}
