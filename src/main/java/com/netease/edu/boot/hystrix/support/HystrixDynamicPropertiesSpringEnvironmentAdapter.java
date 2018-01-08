package com.netease.edu.boot.hystrix.support;

import com.netflix.hystrix.strategy.properties.HystrixDynamicProperties;
import com.netflix.hystrix.strategy.properties.HystrixDynamicProperty;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

/**
 * @author hzfjd
 * @create 17/12/24
 */
public class HystrixDynamicPropertiesSpringEnvironmentAdapter implements HystrixDynamicProperties,EnvironmentAware{

    public HystrixDynamicPropertiesSpringEnvironmentAdapter(){
    }

    private static Environment s_environment;

    private static Logger logger = LoggerFactory.getLogger(HystrixDynamicPropertiesSpringEnvironmentAdapter.class);

    @Override
    public HystrixDynamicProperty<String> getString(final String name,final String fallback) {

        HystrixDynamicProperty<String> hp = new HystrixDynamicProperty<String>() {

            @Override
            public String get() {
                String propValue = null;
                if (s_environment != null) {
                    propValue = s_environment.getProperty(name);
                }
                if (StringUtils.isBlank(propValue)) {
                    propValue = fallback;
                }
                return propValue;
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public void addCallback(Runnable callback) {

            }
        };



        return hp;
    }

    @Override public HystrixDynamicProperty<Integer> getInteger(final String name,final Integer fallback) {





        HystrixDynamicProperty<Integer> hp=new HystrixDynamicProperty<Integer>(){

            @Override
            public Integer get() {
                String propValue=null;
                if (s_environment!=null){
                    propValue= s_environment.getProperty(name);
                }

                Integer propIntValue=null;
                if (StringUtils.isNotBlank(propValue)){
                    try{
                        propIntValue= Integer.parseInt(propValue);
                    }catch (Exception e){
                        logger.warn("parse property value of ({}) error",name,e);
                    }
                }


                if (propIntValue==null){
                    propIntValue=fallback;
                }

                return propIntValue;
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public void addCallback(Runnable callback) {

            }
        };
        return hp;
    }

    @Override public HystrixDynamicProperty<Long> getLong(final  String name,final Long fallback) {



        HystrixDynamicProperty<Long> hp=new HystrixDynamicProperty<Long>(){

            @Override
            public Long get() {
                String propValue=null;
                if (s_environment!=null){
                    propValue= s_environment.getProperty(name);
                }

                Long propObjectValue=null;
                if (StringUtils.isNotBlank(propValue)){
                    try{
                        propObjectValue= Long.parseLong(propValue);
                    }catch (Exception e){
                        logger.warn("parse property value of ({}) error",name,e);
                    }
                }


                if (propObjectValue==null){
                    propObjectValue=fallback;
                }

                return propObjectValue;
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public void addCallback(Runnable callback) {

            }
        };
        return hp;
    }

    @Override public HystrixDynamicProperty<Boolean> getBoolean(final String name,final Boolean fallback) {



        HystrixDynamicProperty<Boolean> hp=new HystrixDynamicProperty<Boolean>(){

            @Override
            public Boolean get() {
                String propValue=null;
                if (s_environment!=null){
                    propValue= s_environment.getProperty(name);
                }

                Boolean propObjectValue=null;
                if (StringUtils.isNotBlank(propValue)){
                    try{
                        propObjectValue= Boolean.parseBoolean(propValue);
                    }catch (Exception e){
                        logger.warn("parse property value of ({}) error",name,e);
                    }
                }


                if (propObjectValue==null){
                    propObjectValue=fallback;
                }

                return propObjectValue;
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public void addCallback(Runnable callback) {

            }
        };
        return hp;
    }

    @Override
    public void setEnvironment(Environment environment) {

        if (s_environment!=null){
            return;
        }
        synchronized (HystrixDynamicPropertiesSpringEnvironmentAdapter.class){
            if (s_environment!=null){
                return;
            }
            s_environment=environment;
        }
    }
}
