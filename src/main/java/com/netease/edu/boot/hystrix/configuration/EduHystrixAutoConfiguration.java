package com.netease.edu.boot.hystrix.configuration;/**
 * Created by hzfjd on 17/12/24.
 */

import com.netease.edu.boot.hystrix.aop.aspectj.DefaultHystrixCommandApiControllerAspect;
import com.netease.edu.boot.hystrix.aop.aspectj.DefaultHystrixCommandDwrAspect;
import com.netease.edu.boot.hystrix.aop.aspectj.DefaultHystrixCommandFrontControllerAspect;
import com.netease.edu.boot.hystrix.core.EduHystrixCommandProperties;
import com.netease.edu.boot.hystrix.core.FallbackFactory;
import com.netease.edu.boot.hystrix.core.OriginApplicationNameResolver;
import com.netease.edu.boot.hystrix.core.SpringFallbackFactory;
import com.netease.edu.boot.hystrix.core.constants.HystrixBeanNameContants;
import com.netease.edu.boot.hystrix.support.HystrixDynamicPropertiesSpringEnvironmentAdapter;
import com.netease.edu.boot.hystrix.support.OriginApplicationNameControllerResolver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * @author hzfjd
 * @create 17/12/24
 */
@Configuration
public class EduHystrixAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public FallbackFactory springFallbackFactory(){
        return new SpringFallbackFactory();
    }

    @Bean
    @ConditionalOnMissingBean
    public HystrixDynamicPropertiesSpringEnvironmentAdapter hystrixDynamicPropertiesSpringEnvironmentAdapter() {
        return new HystrixDynamicPropertiesSpringEnvironmentAdapter();
    }

    @Bean
    @ConditionalOnMissingBean
    public EduHystrixCommandProperties eduHystrixCommandProperties() {
        return new EduHystrixCommandProperties();
    }

    @Bean
    @ConditionalOnClass(DispatcherServlet.class)
    @ConditionalOnMissingBean(name = { HystrixBeanNameContants.ORIGIN_APPLICATION_NAME_CONTROLLER_RESOLVER_BEAN_NAME })
    public OriginApplicationNameResolver originApplicationNameControllerResolver() {
        return new OriginApplicationNameControllerResolver();
    }


    @Bean
    @ConditionalOnMissingBean(name = {
            HystrixBeanNameContants.HYSTRIX_COMMAND_APPLICATION_API_CONTROLLER_ASPECT_BEAN_NAME })
    public DefaultHystrixCommandApiControllerAspect defaultHystrixCommandApiControllerAspect() {
        DefaultHystrixCommandApiControllerAspect defaultHystrixCommandApiControllerAspect = new DefaultHystrixCommandApiControllerAspect();
        defaultHystrixCommandApiControllerAspect.setOriginApplicationNameResolver(
                originApplicationNameControllerResolver());
        return defaultHystrixCommandApiControllerAspect;
    }

    // @Bean
    @ConditionalOnMissingBean(name={HystrixBeanNameContants.HYSTRIX_COMMAND_APPLICATION_DWR_ASPECT_BEAN_NAME})
    @ConditionalOnClass(name={"org.directwebremoting.servlet.UrlProcessor"})
    public DefaultHystrixCommandDwrAspect defaultHystrixCommandDwrAspect(){
       return new DefaultHystrixCommandDwrAspect();
    }

    //@Bean
    @ConditionalOnMissingBean(name={HystrixBeanNameContants.HYSTRIX_COMMAND_APPLICATION_FRONT_CONTROLLER_ASPECT_BEAN_NAME})
    public DefaultHystrixCommandFrontControllerAspect defaultHystrixCommandFrontControllerAspect(){
        return new DefaultHystrixCommandFrontControllerAspect();
    }
}
