package com.netease.edu.eds.trace.clientdemo;/**
                                              * Created by hzfjd on 18/1/8.
                                              */

import com.netease.edu.eds.trace.clientdemo.message.stream.binding.ShuffleStreamBindingForClient;
import com.netease.edu.web.health.HealthCheckProcessor;
import com.netease.edu.web.health.servlet.HealthCheckServlet;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.Ordered;

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
                                     EmbeddedMongoAutoConfiguration.class, RabbitAutoConfiguration.class })
@EnableFeignClients
@EnableDiscoveryClient
@ImportResource("classpath:applicationContext-client.xml")
@EnableBinding({ ShuffleStreamBindingForClient.class })
public class TracerClientDemoApplication {

    public static void main(String[] args) throws InterruptedException {

        new SpringApplicationBuilder().bannerMode(Banner.Mode.OFF).sources(TracerClientDemoApplication.class).run(args);

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
    public HealthCheckProcessor defaultHealthCheckProcessor() {
        return () -> {
            return true;
        };
    }

}
