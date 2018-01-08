package com.netease.edu.boot.hystrix.core;/**
 * Created by hzfjd on 18/1/5.
 */

/**
 * @author hzfjd
 * @create 18/1/5
 */
public interface FallbackFactory {

    public <T> T getFallback(Class<T> targetType);

    public <T> T getFallback(Class<T> targetType, boolean providerSide);

}
