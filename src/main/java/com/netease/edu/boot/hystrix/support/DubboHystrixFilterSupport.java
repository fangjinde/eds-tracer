package com.netease.edu.boot.hystrix.support;/**
 * Created by hzfjd on 18/1/8.
 */

import com.alibaba.dubbo.rpc.*;
import com.alibaba.dubbo.rpc.support.RpcUtils;
import com.netease.ccyctrl.exception.ConcurrencyOverflowException;
import com.netease.edu.boot.hystrix.core.HystrixExecutionContext;
import com.netease.edu.util.exceptions.EduRuntimeException;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.exception.HystrixBadRequestException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author hzfjd
 * @create 18/1/8
 */
public class DubboHystrixFilterSupport {


    public static Result invokeWithHystrix(final Invoker<?> invoker,final Invocation invocation,   HystrixCommand.Setter setter ,final Object fallbackFinal,final boolean providerSide) throws
                                                                                                                                                                                        RpcException {
        HystrixCommand<Result> hystrixCommand = new HystrixCommand<Result>(setter) {

            @Override
            protected Result run() throws Exception {
                try {
                    Result result=invoker.invoke(invocation);
                    if(result.hasException()){
                        processOnIngorableException(result.getException());
                    }
                    return result;
                } catch (Exception e) {
                    processOnIngorableException(e);
                    throw e;
                } catch (Throwable t) {
                    throw (Error) t;
                }
            }

            private void processOnIngorableException(Throwable e){
                if (isIgnoredException(e)){
                    throw new HystrixBadRequestException(String.format("dubbo %s invoke ignorable exception.",providerSide?"provider":"consumer"),e);
                }
            }

            private boolean isIgnoredException(Throwable e){
                if (e instanceof EduRuntimeException){
                    return true;
                }
                if (e instanceof ConcurrencyOverflowException){
                    return true;
                }
                return false;
            }

            @Override
            protected Result getFallback() {
                if (fallbackFinal == null) {
                    return super.getFallback();
                }
                try {

                    Object fallbackResult = null;
                    Throwable executionException = getFailedExecutionException();
                    HystrixExecutionContext.setExecutionException(executionException);
                    try {
                        Method method = fallbackFinal.getClass().getMethod(RpcUtils.getMethodName(invocation),
                                                                           RpcUtils.getParameterTypes(
                                                                                   invocation));
                        fallbackResult = method.invoke(fallbackFinal, RpcUtils.getArguments(invocation));
                    } catch (NoSuchMethodException e) {
                        return super.getFallback();
                    } finally {
                        HystrixExecutionContext.resetExecutionException();
                    }

                    return new RpcResult(fallbackResult);

                } catch (IllegalAccessException e) {
                    // shouldn't happen as method is public due to being an interface
                    throw new AssertionError(e);
                } catch (InvocationTargetException e) {
                    // Exceptions on fallback are tossed by Hystrix
                    throw new AssertionError(e.getCause());
                }
            }
        };

        return hystrixCommand.execute();
    }
}
