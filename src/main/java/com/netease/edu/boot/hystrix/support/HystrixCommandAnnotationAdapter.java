package com.netease.edu.boot.hystrix.support;

import com.netease.edu.boot.hystrix.annotation.EduHystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import com.netflix.hystrix.contrib.javanica.annotation.ObservableExecutionMode;

import java.lang.annotation.Annotation;

/**
 * @author hzfjd
 * @create 17/12/22
 */
public class HystrixCommandAnnotationAdapter implements HystrixCommand {

    private EduHystrixCommand eduHystrixCommand;

    public HystrixCommandAnnotationAdapter(EduHystrixCommand eduHystrixCommand) {

        //兼容MetaHolder判断逻辑
        if (eduHystrixCommand == null) {
            eduHystrixCommand = new EduHystrixCommand() {

                @Override public Class<? extends Annotation> annotationType() {
                    return null;
                }

                @Override public String groupKey() {
                    return "";
                }

                @Override public String commandKey() {
                    return "";
                }

                @Override public String threadPoolKey() {
                    return "";
                }

                @Override public String fallbackMethod() {
                    return "";
                }

                @Override public HystrixProperty[] commandProperties() {
                    return new HystrixProperty[0];
                }

                @Override public HystrixProperty[] threadPoolProperties() {
                    return new HystrixProperty[0];
                }

                @Override public Class<? extends Throwable>[] ignoreExceptions() {
                    return new Class[0];
                }

                @Override public ObservableExecutionMode observableExecutionMode() {
                    return ObservableExecutionMode.EAGER;
                }
            };
        }

        this.eduHystrixCommand=eduHystrixCommand;
    }

    @Override
    public String groupKey() {
        return eduHystrixCommand.groupKey();
    }

    @Override public String commandKey() {
        return eduHystrixCommand.commandKey();
    }

    @Override public String threadPoolKey() {
        return eduHystrixCommand.threadPoolKey();
    }

    @Override public String fallbackMethod() {
        return eduHystrixCommand.fallbackMethod();
    }

    @Override public HystrixProperty[] commandProperties() {
        return eduHystrixCommand.commandProperties();
    }

    @Override public HystrixProperty[] threadPoolProperties() {
        return eduHystrixCommand.threadPoolProperties();
    }

    @Override public Class<? extends Throwable>[] ignoreExceptions() {
        return eduHystrixCommand.ignoreExceptions();
    }

    @Override public ObservableExecutionMode observableExecutionMode() {
        return eduHystrixCommand.observableExecutionMode();
    }

    @Override public Class<? extends Annotation> annotationType() {
        return eduHystrixCommand.annotationType();
    }
}
