package com.netease.edu.boot.hystrix.support;/**
 * Created by hzfjd on 18/1/10.
 */

import com.netease.edu.boot.hystrix.core.HystrixIgnoreExceptionProvider;
import com.netease.edu.util.exceptions.FrontNotifiableRuntimeException;

/**
 * @author hzfjd
 * @create 18/1/10
 */
public class DefaultHystrixIgnoreSuperExceptionProvider implements HystrixIgnoreExceptionProvider {

    @Override
    public Class<? extends Throwable>[] getIgnorable() {
        return new Class[]{ FrontNotifiableRuntimeException.class};
    }
}
