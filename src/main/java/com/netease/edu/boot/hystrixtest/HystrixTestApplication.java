package com.netease.edu.boot.hystrixtest;/**
 * Created by hzfjd on 18/1/8.
 */

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.actuate.autoconfigure.HealthIndicatorAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.Ordered;

/**
 * @author hzfjd
 * @create 18/1/8
 */

@SpringBootConfiguration
@ComponentScan
@EnableAutoConfiguration(exclude = {DataSourceTransactionManagerAutoConfiguration.class,MongoDataAutoConfiguration.class,MongoRepositoriesAutoConfiguration.class,RedisAutoConfiguration.class,HealthIndicatorAutoConfiguration.RedisHealthIndicatorConfiguration.class,RedisRepositoriesAutoConfiguration.class,EmbeddedMongoAutoConfiguration.class,RabbitAutoConfiguration.class,HealthIndicatorAutoConfiguration.RabbitHealthIndicatorConfiguration.class})
@EnableFeignClients
@EnableDiscoveryClient
@ImportResource("classpath:applicationContext-dubbo-provider.xml")
public class HystrixTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(HystrixTestApplication.class,args);
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer(){
        PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer=new PropertySourcesPlaceholderConfigurer();
        propertySourcesPlaceholderConfigurer.setIgnoreUnresolvablePlaceholders(true);
        propertySourcesPlaceholderConfigurer.setOrder(Ordered.LOWEST_PRECEDENCE-1);
        return propertySourcesPlaceholderConfigurer;

    }
}
