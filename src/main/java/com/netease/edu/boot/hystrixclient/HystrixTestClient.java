package com.netease.edu.boot.hystrixclient;/**
 * Created by hzfjd on 18/1/8.
 */

import com.netease.edu.boot.hystrixtest.service.HystrixScratchService;
import org.springframework.boot.Banner;
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
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.context.ConfigurableApplicationContext;
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
@EnableAutoConfiguration(exclude = { DataSourceTransactionManagerAutoConfiguration.class,
                                     MongoDataAutoConfiguration.class, MongoRepositoriesAutoConfiguration.class,
                                     RedisAutoConfiguration.class,
                                     HealthIndicatorAutoConfiguration.RedisHealthIndicatorConfiguration.class,
                                     RedisRepositoriesAutoConfiguration.class, EmbeddedMongoAutoConfiguration.class,
                                     RabbitAutoConfiguration.class,
                                     HealthIndicatorAutoConfiguration.RabbitHealthIndicatorConfiguration.class })
@EnableFeignClients
@EnableDiscoveryClient
@ImportResource("classpath:applicationContext-dubbo-consumer.xml")
public class HystrixTestClient {

    public static void main(String[] args) {

        ConfigurableApplicationContext applicationContext = new SpringApplicationBuilder().bannerMode(
                Banner.Mode.OFF).sources(HystrixTestClient.class).profiles("client").run(args);
        HystrixScratchService hystrixScratchService = applicationContext.getBean(HystrixScratchService.class);

        String v1 = testSafely(hystrixScratchService, 1);
        String v2 = testSafely(hystrixScratchService,2);
        String v3 =testSafely(hystrixScratchService, 3);
        String vnull = testSafely(hystrixScratchService, null);

    }

    public static String testSafely(HystrixScratchService hystrixScratchService,Integer testcase){
        try{
           return hystrixScratchService.echo(testcase);
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }

    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer = new PropertySourcesPlaceholderConfigurer();
        propertySourcesPlaceholderConfigurer.setIgnoreUnresolvablePlaceholders(true);
        propertySourcesPlaceholderConfigurer.setOrder(Ordered.LOWEST_PRECEDENCE - 1);
        return propertySourcesPlaceholderConfigurer;

    }

}
