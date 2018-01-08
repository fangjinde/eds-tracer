package com.netease.edu.boot.hystrix.support;/**
 * Created by hzfjd on 18/1/3.
 */

import com.netflix.hystrix.contrib.javanica.annotation.DefaultProperties;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;

import java.lang.annotation.Annotation;

/**
 * @author hzfjd
 * @create 18/1/3
 */
public class DefaultPropertiesForged implements DefaultProperties {

    private HystrixProperty[] commandProperties;

    public DefaultPropertiesForged(HystrixProperty commandHystrixProperty){
        commandProperties=new HystrixProperty[1];
        commandProperties[0]=commandHystrixProperty;
    }

    @Override
    public String groupKey() {
        return "";
    }

    @Override
    public String threadPoolKey() {
        return "";
    }

    @Override
    public HystrixProperty[] commandProperties() {
        return commandProperties;
    }

    @Override
    public HystrixProperty[] threadPoolProperties() {
        return new HystrixProperty[0];
    }

    @Override
    public Class<? extends Throwable>[] ignoreExceptions() {
        return new Class[0];
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return null;
    }
}
