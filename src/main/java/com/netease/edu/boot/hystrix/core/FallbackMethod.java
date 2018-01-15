package com.netease.edu.boot.hystrix.core;/**
 * Created by hzfjd on 18/1/15.
 */

import java.lang.reflect.Method;

/**
 * @author hzfjd
 * @create 18/1/15
 */
public class FallbackMethod {

    private final Method  method;
    private final boolean extended;

    public boolean isExtended() {
        return extended;
    }

    public Method getMethod() {
        return method;
    }

    public static final FallbackMethod ABSENT = new FallbackMethod(null, false);

    public FallbackMethod(Method method) {
        this(method, false);
    }

    public FallbackMethod(Method method, boolean extended) {
        this.method = method;
        this.extended = extended;
    }

    public boolean isPresent() {
        return method != null;
    }
}
