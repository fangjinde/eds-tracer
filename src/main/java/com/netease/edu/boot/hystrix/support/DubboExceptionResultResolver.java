package com.netease.edu.boot.hystrix.support;/**
 * Created by hzfjd on 18/1/10.
 */

import com.alibaba.dubbo.rpc.Result;
import com.netease.edu.boot.hystrix.core.ExceptionResultResolver;

/**
 * @author hzfjd
 * @create 18/1/10
 */
public class DubboExceptionResultResolver implements ExceptionResultResolver<Throwable,Result>  {

    public boolean hasException(Result result) {
        return result.hasException();
    }

    public Throwable getException(Result result) {
        return result.getException();
    }
}
