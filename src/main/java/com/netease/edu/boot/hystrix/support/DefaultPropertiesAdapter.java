package com.netease.edu.boot.hystrix.support;/**
 * Created by hzfjd on 18/1/3.
 */

import com.netflix.hystrix.contrib.javanica.annotation.DefaultProperties;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import org.apache.commons.lang.StringUtils;

import java.lang.annotation.Annotation;

/**
 * @author hzfjd
 * @create 18/1/3
 */
public class DefaultPropertiesAdapter implements DefaultProperties {

    private DefaultProperties target;
    private HystrixProperty[] overridedCommandHystrixPropertys;

    public DefaultPropertiesAdapter(DefaultProperties target) {
        this.target = target;
    }

    public DefaultPropertiesAdapter(DefaultProperties target,HystrixProperty commandHystrixProperty) {
        this.target = target;

        if (commandHystrixProperty==null|| StringUtils.isBlank(commandHystrixProperty.name())){
            return;
        }

        boolean existed=false;
        for (HystrixProperty hp:target.commandProperties()){
            if (commandHystrixProperty.name().equals(hp.name())){
                existed=true;
                break;
            }
        }
        if (!existed){
            overridedCommandHystrixPropertys=new HystrixProperty[target.commandProperties().length+1];
        }


        for (int i=0;i<target.commandProperties().length;i++){
            overridedCommandHystrixPropertys[i]=target.commandProperties()[i];
        }
        overridedCommandHystrixPropertys[target.commandProperties().length]=commandHystrixProperty;


    }

    @Override public String groupKey() {
        return target.groupKey();
    }

    @Override public String threadPoolKey() {
        return target.threadPoolKey();
    }

    @Override public HystrixProperty[] commandProperties() {
        if (overridedCommandHystrixPropertys!=null){
            return overridedCommandHystrixPropertys;
        }
        return target.commandProperties();
    }

    @Override public HystrixProperty[] threadPoolProperties() {
        return target.threadPoolProperties();
    }

    @Override public Class<? extends Throwable>[] ignoreExceptions() {
        return target.ignoreExceptions();
    }

    @Override public Class<? extends Annotation> annotationType() {
        return target.annotationType();
    }
}
