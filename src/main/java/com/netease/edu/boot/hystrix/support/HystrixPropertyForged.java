package com.netease.edu.boot.hystrix.support;/**
 * Created by hzfjd on 18/1/3.
 */

import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;

import java.lang.annotation.Annotation;

/**
 * @author hzfjd
 * @create 18/1/3
 */
public class HystrixPropertyForged implements HystrixProperty {

    public static HystrixProperty SEMAPHORE_HYSTRIX_PROPERTY= new HystrixPropertyForged("execution.isolation.strategy","SEMAPHORE");

    private String name;
    private String value;

    public HystrixPropertyForged(String name, String value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String value() {
        return value;
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return null;
    }
}
