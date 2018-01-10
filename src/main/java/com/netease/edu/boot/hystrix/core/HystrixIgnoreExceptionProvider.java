package com.netease.edu.boot.hystrix.core;

/**
 * Created by hzfjd on 18/1/10.
 */
public interface HystrixIgnoreExceptionProvider {

    Class<? extends Throwable>[] getIgnorable();
}
