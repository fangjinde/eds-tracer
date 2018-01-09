package com.netease.edu.boot.hystrix.support;/**
 * Created by hzfjd on 18/1/8.
 */

import com.alibaba.dubbo.rpc.*;
import com.alibaba.dubbo.rpc.support.RpcUtils;
import com.netease.edu.boot.hystrix.core.HystrixExecutionContext;
import com.netease.edu.util.exceptions.FrontNotifiableRuntimeException;
import com.netease.edu.util.exceptions.SystemErrorRuntimeException;
import com.netflix.hystrix.HystrixCommand;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author hzfjd
 * @create 18/1/8
 */
public class DubboHystrixFilterSupport {

    public static boolean isUserBadRequestException(Throwable e) {
        if (e instanceof FrontNotifiableRuntimeException) {
            return true;
        }
        return false;
    }

    public static Result invokeWithHystrix(final Invoker<?> invoker, final Invocation invocation,
                                           HystrixCommand.Setter setter, final Object fallbackFinal,
                                           final boolean providerSide) throws
                                                                       RpcException {
        HystrixCommand<Result> hystrixCommand = new HystrixCommand<Result>(setter) {

            @Override
            protected Result run() throws Exception {
                Result result = invoker.invoke(invocation);
                if (result.hasException() && !isUserBadRequestException(
                        result.getException())) {
                    //just let Hystrix to record
                    throw new DubboInvokeException(result.getException());
                }
                return result;
            }

            @Override
            protected Result getFallback() {
                if (fallbackFinal == null) {
                    return super.getFallback();
                }

                Object fallbackResult = null;
                Throwable commandExecutionException = getFailedExecutionException();
                Throwable targetException = commandExecutionException;
                if (commandExecutionException instanceof DubboInvokeException) {
                    targetException = commandExecutionException.getCause();
                }
                HystrixExecutionContext.setExecutionException(targetException);
                try {
                    Method method = fallbackFinal.getClass().getMethod(RpcUtils.getMethodName(invocation),
                                                                       RpcUtils.getParameterTypes(
                                                                               invocation));
                    fallbackResult = method.invoke(fallbackFinal, RpcUtils.getArguments(invocation));
                } catch (NoSuchMethodException e) {
                    return super.getFallback();
                } catch (IllegalAccessException e) {
                    return super.getFallback();
                } catch (IllegalArgumentException e) {
                    return super.getFallback();
                } catch (InvocationTargetException e) {
                    throw new SystemErrorRuntimeException("InvocationTargetException on fallback",e);
                } finally {
                    HystrixExecutionContext.resetExecutionException();
                }

                return new RpcResult(fallbackResult);

            }
        };

        return hystrixCommand.execute();
    }

    static class DubboInvokeException extends Exception {

        DubboInvokeException(Throwable cause) {
            super(cause);
        }
    }
}
