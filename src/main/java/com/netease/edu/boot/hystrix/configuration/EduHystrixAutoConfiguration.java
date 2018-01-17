package com.netease.edu.boot.hystrix.configuration;/**
 * Created by hzfjd on 17/12/24.
 */

import com.netease.edu.boot.hystrix.aop.aspectj.DefaultHystrixCommandApiControllerAspect;
import com.netease.edu.boot.hystrix.aop.aspectj.DefaultHystrixCommandDwrAspect;
import com.netease.edu.boot.hystrix.aop.aspectj.DefaultHystrixCommandUIControllerAspect;
import com.netease.edu.boot.hystrix.core.*;
import com.netease.edu.boot.hystrix.core.constants.HystrixBeanNameContants;
import com.netease.edu.boot.hystrix.support.*;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.cloud.client.actuator.HasFeatures;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.DispatcherServlet;

import javax.servlet.http.HttpServletRequest;

/**
 * @author hzfjd
 * @create 17/12/24
 */
@Configuration
public class EduHystrixAutoConfiguration {

    @Bean(name = HystrixBeanNameContants.HYSTRIX_COMMAND_ASPECT_RESULT_EXCEPTION_CHECKERS)
    @ConditionalOnMissingBean(name = HystrixBeanNameContants.HYSTRIX_COMMAND_ASPECT_RESULT_EXCEPTION_CHECKERS)
    public ResultExceptionChecker hystrixCommandAspectResultExceptionCheckers() {
        return new ResultExceptionCheckerComposite();
    }

    @Configuration
    @ConditionalOnProperty(value = "hystrix.stream.endpoint.enabled", matchIfMissing = true)
    @ConditionalOnWebApplication
    @ConditionalOnClass({ Endpoint.class, EduHystrixMetricsStreamServlet.class })
    protected static class HystrixWebConfiguration {

        @Bean
        public EduHystrixStreamEndpoint hystrixStreamEndpoint() {
            return new EduHystrixStreamEndpoint();
        }

        @Bean
        public HasFeatures hystrixFeature() {
            return HasFeatures.namedFeature("Hystrix Stream Servlet", EduHystrixStreamEndpoint.class);
        }
    }

    class DefaultResultExceptionCheckers {

        @Bean
        @ConditionalOnClass(name = { "com.netease.edu.agent.mobile.common.vo.MobReturnVo" })
        public ResultExceptionChecker mobReturnVoResultExceptionChecker() {
            return new MobReturnVoResultExceptionChecker();
        }

        @Bean
        @ConditionalOnClass(name = { "com.netease.edu.web.viewer.ResponseView" })
        public ResultExceptionChecker responseViewResultExceptionChecker() {
            return new ResponseViewResultExceptionChecker();
        }
    }

    @Bean
    @ConditionalOnMissingBean
    public EduBadRequestExceptionIdentifier eduBadRequestExceptionIdentifier() {
        return new EduBadRequestExceptionIdentifier();
    }

    @Bean(name = HystrixBeanNameContants.EDU_HYSTRIX_IGNORABLE_SUPER_EXCEPTIONS_BEAN_NAME)
    @ConditionalOnMissingBean(name = HystrixBeanNameContants.EDU_HYSTRIX_IGNORABLE_SUPER_EXCEPTIONS_BEAN_NAME)
    public HystrixIgnoreExceptionProvider defaultHystrixIgnoreExceptionProvider() {
        return new DefaultHystrixIgnoreSuperExceptionProvider();
    }

    @Bean
    @ConditionalOnMissingBean
    public FallbackFactory fallbackFactory() {
        return new SpringFallbackFactory();
    }

    @Bean
    @ConditionalOnMissingBean
    public static DubboReferenceRegistryProcessor dubboReferenceRegistryProcessor() {
        return new DubboReferenceRegistryProcessor();
    }

    @Bean
    @ConditionalOnMissingBean
    public HystrixDynamicPropertiesSpringEnvironmentAdapter hystrixDynamicPropertiesSpringEnvironmentAdapter() {
        return new HystrixDynamicPropertiesSpringEnvironmentAdapter();
    }

    @Bean
    @ConditionalOnMissingBean
    public EduHystrixGlobalProperties eduHystrixGlobalProperties() {
        return new EduHystrixGlobalProperties();
    }

    @ConditionalOnClass({ DispatcherServlet.class })
    @ConditionalOnBean({ HttpServletRequest.class })
    @ConditionalOnMissingBean
    public OriginApplicationNameResolver originApplicationNameControllerResolver() {
        return new OriginApplicationNameControllerResolver();
    }

    @Bean(name = HystrixBeanNameContants.HYSTRIX_COMMAND_APPLICATION_API_CONTROLLER_ASPECT_BEAN_NAME)
    @ConditionalOnMissingBean(name = {
            HystrixBeanNameContants.HYSTRIX_COMMAND_APPLICATION_API_CONTROLLER_ASPECT_BEAN_NAME })
    @ConditionalOnBean({ HttpServletRequest.class })
    public DefaultHystrixCommandApiControllerAspect defaultHystrixCommandApiControllerAspect(
            OriginApplicationNameResolver originApplicationNameControllerResolver) {
        DefaultHystrixCommandApiControllerAspect defaultHystrixCommandApiControllerAspect = new DefaultHystrixCommandApiControllerAspect();
        defaultHystrixCommandApiControllerAspect.setOriginApplicationNameResolver(
                originApplicationNameControllerResolver);
        return defaultHystrixCommandApiControllerAspect;
    }

    @Bean(name = HystrixBeanNameContants.HYSTRIX_COMMAND_APPLICATION_DWR_ASPECT_BEAN_NAME)
    @ConditionalOnMissingBean(name = { HystrixBeanNameContants.HYSTRIX_COMMAND_APPLICATION_DWR_ASPECT_BEAN_NAME })
    @ConditionalOnClass(name = { "org.directwebremoting.servlet.UrlProcessor" })
    public DefaultHystrixCommandDwrAspect defaultHystrixCommandDwrAspect() {
        return new DefaultHystrixCommandDwrAspect();
    }

    @Bean(name = HystrixBeanNameContants.HYSTRIX_COMMAND_APPLICATION_FRONT_CONTROLLER_ASPECT_BEAN_NAME)
    @ConditionalOnMissingBean(name = {
            HystrixBeanNameContants.HYSTRIX_COMMAND_APPLICATION_FRONT_CONTROLLER_ASPECT_BEAN_NAME })
    public DefaultHystrixCommandUIControllerAspect defaultHystrixCommandFrontControllerAspect() {
        return new DefaultHystrixCommandUIControllerAspect();
    }
}
