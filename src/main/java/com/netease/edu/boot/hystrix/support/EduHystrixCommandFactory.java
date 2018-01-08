package com.netease.edu.boot.hystrix.support;


import com.netflix.hystrix.HystrixInvokable;
import com.netflix.hystrix.contrib.javanica.collapser.CommandCollapser;
import com.netflix.hystrix.contrib.javanica.command.GenericCommand;
import com.netflix.hystrix.contrib.javanica.command.GenericObservableCommand;
import com.netflix.hystrix.contrib.javanica.command.MetaHolder;

/**
 * Created by dmgcodevil.
 * @author hzfjd
 */
public class EduHystrixCommandFactory {

    private static final EduHystrixCommandFactory INSTANCE = new EduHystrixCommandFactory();

    private EduHystrixCommandFactory() {

    }

    public static EduHystrixCommandFactory getInstance() {
        return INSTANCE;
    }

    public HystrixInvokable create(MetaHolder metaHolder) {
        HystrixInvokable executable;
        if (metaHolder.isCollapserAnnotationPresent()) {
            executable = new CommandCollapser(metaHolder);
        } else if (metaHolder.isObservable()) {
            executable = new GenericObservableCommand(EduHystrixCommandBuilderFactory.getInstance().create(metaHolder));
        } else {
            executable = new GenericCommand(EduHystrixCommandBuilderFactory.getInstance().create(metaHolder));
        }
        return executable;
    }

    public HystrixInvokable createDelayed(MetaHolder metaHolder) {
        HystrixInvokable executable;
        if (metaHolder.isObservable()) {
            executable = new GenericObservableCommand(EduHystrixCommandBuilderFactory.getInstance().create(metaHolder));
        } else {
            executable = new GenericCommand(EduHystrixCommandBuilderFactory.getInstance().create(metaHolder));
        }
        return executable;
    }
}
