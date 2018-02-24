package com.netease.edu.boot.hystrix.core;/**
 * Created by hzfjd on 18/1/10.
 */

import com.netease.edu.boot.hystrix.core.exception.CommandExecuteException;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.exception.HystrixBadRequestException;

/**
 * @author hzfjd
 * @create 18/1/10
 */
public class EduHystrixExecutor {

    public static <R> R executeWithHystrix(final CommandAction<R> commandAction,
                                  final CommandAction<R> fallbackAction,
                                  HystrixCommand.Setter setter,
                                  final ResultExceptionChecker<R> resultExceptionChecker) {
        HystrixCommand<R> hystrixCommand = new HystrixCommand<R>(setter) {

            @Override
            protected R run() throws Exception {
                R result = null;
                try {
                    result = commandAction.execute();
                } catch (CommandExecuteException e) {
                    if (EduBadRequestExceptionIdentifier.isIgnorable(
                            e.getCause())) {
                        throw new HystrixBadRequestException("", e.getCause());
                    }
                    throw e;
                }

                //if there is implicit exception in result, throw CommandExecuteException with origin result
                if (resultExceptionChecker != null) {
                    resultExceptionChecker.check(result);
                }
                return result;
            }

            @Override
            protected R getFallback() {

                Throwable commandExecutionException = getFailedExecutionException();
                Throwable targetException = commandExecutionException;
                R originResult = null;
                if (commandExecutionException instanceof CommandExecuteException) {
                    targetException = commandExecutionException.getCause();
                    originResult = ((CommandExecuteException) commandExecutionException).getResult();
                }

                if (fallbackAction == null) {
                    //no fallback, use the origin result if exists
                    if (originResult != null) {
                        return originResult;
                    }
                    //hystrix will ignore this fallback exception, so whatever it is.
                    throw new RuntimeException("No fallback available.", commandExecutionException);
                }

                R fallbackResult = null;
                try {
                    HystrixExecutionContext.setExecutionException(targetException);
                    fallbackResult = fallbackAction.execute();
                    return fallbackResult;
                } catch (RuntimeException fallbackExecuteException) {
                    //fallback failed(that means throw exception), use the origin result if exists
                    if (originResult != null) {
                        return originResult;
                    }
                    //hystrix will ignore this fallback exception, so whatever it is.
                    throw fallbackExecuteException;

                } finally {
                    HystrixExecutionContext.resetExecutionException();
                }

            }
        };

        try{
            return hystrixCommand.execute();
        }catch (HystrixBadRequestException bre){
           Throwable cause= bre.getCause();
            if (cause instanceof RuntimeException){
                throw (RuntimeException)cause;
            }else{
                throw new RuntimeException("biz throw HystrixBadRequestException without any Runtime cause",cause);
            }
        }


    }
}
