package com.netease.edu.boot.hystrix.support;/**
 * Created by hzfjd on 17/12/26.
 */

import com.netease.edu.boot.hystrix.annotation.EduHystrixCommand;
import com.netflix.hystrix.contrib.javanica.utils.FallbackMethod;

import java.lang.reflect.Method;

/**
 * @author hzfjd
 * @create 17/12/26
 */
public class FallbackMethodEduAdapter extends FallbackMethod {

    public FallbackMethodEduAdapter(Method method, boolean extended) {
        super(method, extended);
    }

    @Override
    public boolean isCommand() {
        return getMethod().isAnnotationPresent(EduHystrixCommand.class);
    }


}
