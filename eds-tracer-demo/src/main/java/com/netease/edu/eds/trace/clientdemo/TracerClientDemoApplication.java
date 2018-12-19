package com.netease.edu.eds.trace.clientdemo;/**
                                              * Created by hzfjd on 18/1/8.
                                              */

import com.netease.edu.eds.trace.clientdemo.message.stream.binding.ShuffleStreamBindingForClient;
import com.netease.edu.eds.trace.demo.constants.ApplicationCommandArgs;
import org.apache.commons.collections.CollectionUtils;
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
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.Ordered;

import java.util.ArrayList;
import java.util.List;

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

        String[] argsDiff = { "--spring.application.name=eds-tracer-demo-client" };
        List<String> argsList = new ArrayList<>(args.length + ApplicationCommandArgs.SAME_ARGS.length
                                                + argsDiff.length);
        CollectionUtils.addAll(argsList, args);
        CollectionUtils.addAll(argsList, ApplicationCommandArgs.SAME_ARGS);
        CollectionUtils.addAll(argsList, argsDiff);

        new SpringApplicationBuilder().bannerMode(Banner.Mode.OFF).sources(TracerClientDemoApplication.class).profiles("client").run(argsList.toArray(new String[0]));

    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer = new PropertySourcesPlaceholderConfigurer();
        propertySourcesPlaceholderConfigurer.setIgnoreUnresolvablePlaceholders(true);
        propertySourcesPlaceholderConfigurer.setOrder(Ordered.LOWEST_PRECEDENCE - 1);
        return propertySourcesPlaceholderConfigurer;

    }

}