package com.netease.edu.eds.trace.serve;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.Ordered;

/**
 * @author hzfjd
 * @create 18/12/12
 **/
@SpringBootApplication(exclude = { DataSourceTransactionManagerAutoConfiguration.class,
                                   MongoDataAutoConfiguration.class, MongoRepositoriesAutoConfiguration.class,
                                   RedisRepositoriesAutoConfiguration.class,
                                   EmbeddedMongoAutoConfiguration.class, RabbitAutoConfiguration.class,
                                   SpringDataWebAutoConfiguration.class })
@EnableFeignClients
@EnableDiscoveryClient
@ImportResource({ "classpath:applicationContext-server.xml" })
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

}
