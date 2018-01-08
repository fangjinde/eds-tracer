package com.netease.edu.boot.hystrix.core;/**
 * Created by hzfjd on 18/1/5.
 */

/**
 * @author hzfjd
 * @create 18/1/5
 */
public class HystrixExecutionContext {

    private static ThreadLocal<Throwable> executionExceptionHolder=new ThreadLocal<Throwable>();

    public static Throwable getExecutionException(){
        return executionExceptionHolder.get();
    }

    public static void setExecutionException(Throwable e){
        executionExceptionHolder.set(e);
    }

    public static void resetExecutionException(){
        executionExceptionHolder.set(null);
    }


}
