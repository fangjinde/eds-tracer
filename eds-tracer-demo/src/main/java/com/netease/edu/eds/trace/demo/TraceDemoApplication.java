package com.netease.edu.eds.trace.demo;/**
                                        * Created by hzfjd on 18/1/8.
                                        */

import com.netease.edu.eds.trace.demo.aop.TransactionAspect;
import com.netease.edu.eds.trace.demo.component.DefaultHealthCheckProcessor;
import com.netease.edu.eds.trace.demo.ioc.ExcludeBeanFactoryPostProcessor;
import com.netease.edu.eds.trace.demo.message.stream.binding.ShuffleStreamBinding;
import com.netease.edu.transaction.message.client.config.TransactionMessageClientConfig;
import com.netease.edu.web.health.HealthCheckProcessor;
import com.netease.edu.web.health.servlet.HealthCheckServlet;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.Ordered;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * @author hzfjd
 * @create 18/1/8
 */

@SpringBootConfiguration
@ComponentScan
@EnableAutoConfiguration(exclude = { DataSourceTransactionManagerAutoConfiguration.class,
                                     MongoDataAutoConfiguration.class, MongoRepositoriesAutoConfiguration.class,
                                     RedisAutoConfiguration.class, RedisRepositoriesAutoConfiguration.class,
                                     EmbeddedMongoAutoConfiguration.class, RabbitAutoConfiguration.class,
                                     SpringDataWebAutoConfiguration.class })
@EnableFeignClients
@EnableDiscoveryClient
@ImportResource({ "classpath:applicationContext-server.xml" })
@Import(value = { TransactionMessageClientConfig.class })
@EnableBinding({ ShuffleStreamBinding.class })
public class TraceDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(TraceDemoApplication.class, args);
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer = new PropertySourcesPlaceholderConfigurer();
        propertySourcesPlaceholderConfigurer.setIgnoreUnresolvablePlaceholders(true);
        propertySourcesPlaceholderConfigurer.setOrder(Ordered.LOWEST_PRECEDENCE - 1);
        return propertySourcesPlaceholderConfigurer;

    }

    @Bean
    public HealthCheckServlet healthCheckServlet() {
        HealthCheckServlet healthCheckServlet = new HealthCheckServlet();
        return healthCheckServlet;
    }

    @Bean
    public ServletRegistrationBean healthCheckServletRegistrationBean() {
        ServletRegistrationBean registration = new ServletRegistrationBean();
        registration.setServlet(healthCheckServlet());
        registration.setName("healthCheckServlet");
        Map<String, String> initParams = new HashMap<String, String>();
        initParams.put("allowIps",
                       "127.0.0.1,10.120.152.63,10.120.144.71,172.17.1.18,10.164.132.130,10.122.138.119,10.164.143.133");
        registration.setInitParameters(initParams);
        registration.addUrlMappings("/health/*");
        return registration;
    }

    @Bean
    public HealthCheckProcessor defaultHealthCheckProcessor(){
        return new DefaultHealthCheckProcessor();
    }

    @Bean
    public static ExcludeBeanFactoryPostProcessor excludeBeanFactoryPostProcessor() {
        return new ExcludeBeanFactoryPostProcessor();
    }

    @Bean
    public TransactionAspect TransactionAspect() {
        return new TransactionAspect();
    }

    @Bean
    @LoadBalanced
    public RestTemplate restTemplateDemo(){
        return new RestTemplate();
    }

}
