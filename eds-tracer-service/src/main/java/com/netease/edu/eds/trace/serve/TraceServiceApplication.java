package com.netease.edu.eds.trace.serve;

import com.netease.edu.web.health.servlet.HealthCheckServlet;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.Ordered;

import com.netease.edu.eds.trace.serve.props.ShuffleServerProperties;
import com.netease.edu.eds.trace.serve.props.TraceServerProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * @author hzfjd
 * @create 18/12/12
 **/
@SpringBootApplication(exclude = { DataSourceTransactionManagerAutoConfiguration.class,
                                   MongoDataAutoConfiguration.class, MongoRepositoriesAutoConfiguration.class,
                                   RedisRepositoriesAutoConfiguration.class, EmbeddedMongoAutoConfiguration.class,
                                   RabbitAutoConfiguration.class, SpringDataWebAutoConfiguration.class })
@EnableFeignClients
@EnableDiscoveryClient
@ImportResource({ "classpath:applicationContext-server.xml" })
@EnableConfigurationProperties({ TraceServerProperties.class ,ShuffleServerProperties.class})
public class TraceServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TraceServiceApplication.class, args);
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer = new PropertySourcesPlaceholderConfigurer();
        propertySourcesPlaceholderConfigurer.setIgnoreUnresolvablePlaceholders(true);
        propertySourcesPlaceholderConfigurer.setOrder(Ordered.LOWEST_PRECEDENCE - 1);
        return propertySourcesPlaceholderConfigurer;

    }

    @Bean
    public HealthCheckServlet healthCheckServlet(){
        HealthCheckServlet healthCheckServlet=new HealthCheckServlet();
        return healthCheckServlet;
    }


    @Bean
    public ServletRegistrationBean healthCheckServletRegistrationBean(){
        ServletRegistrationBean registration=new ServletRegistrationBean();
        registration.setServlet(healthCheckServlet());
        registration.setName("healthCheckServlet");
        Map<String,String> initParams=new HashMap<String,String>();
        initParams.put("allowIps","127.0.0.1,10.120.152.63,10.120.144.71,172.17.1.18,10.164.132.130,10.122.138.119,10.164.143.133");
        registration.setInitParameters(initParams);
        registration.addUrlMappings("/health/*");
        return registration;
    }

}
