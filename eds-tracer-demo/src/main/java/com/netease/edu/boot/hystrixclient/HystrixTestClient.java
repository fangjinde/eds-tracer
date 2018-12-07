package com.netease.edu.boot.hystrixclient;/**
                                            * Created by hzfjd on 18/1/8.
                                            */

import com.netease.edu.boot.hystrixclient.message.stream.binding.ShuffleStreamBindingForClient;
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
public class HystrixTestClient {

    public static void main(String[] args) throws InterruptedException {

        //serviceBus
        //--spring.cloud.stream.bindings.shuffleStreamOutput.binder=serviceBus
        //--spring.cloud.stream.bindings.shuffleStreamOutput.destination=shuffleCloudStreamDemoTopic${local_service_version_suffix}

        String[] args2 = { "--spring.cloud.stream.bindings.shuffleStreamOutput.binder=serviceBus","--spring.cloud.stream.bindings.shuffleStreamOutput.destination=shuffleCloudStreamDemoTopic","--spring.cloud.stream.rabbit.bindings.shuffleStreamOutput.producer.prefix=${spring.profiles.active}-","--spring.sleuth.web.additionalSkipPattern=/health/status|/web/echoNoTrace",
                           "--edu-hystrix-demo-web_service_version_suffix=-${spring.profiles.active}",
                           "--hystrix.command.default.execution.isolation.thread.timeoutInMilliseconds=3600000",
                           "--deployAppName=eds-tracer-demo",
                           "--deployAppClusterName=eds-tracer-demo_${spring.profiles.active}",
                           "--local_service_version_suffix=-${spring.profiles.active}",
                           "--remote_service_version_suffix=-${spring.profiles.active}" };
        List<String> argsList = new ArrayList<>(args.length + args2.length);
        CollectionUtils.addAll(argsList, args);
        CollectionUtils.addAll(argsList, args2);

        new SpringApplicationBuilder().bannerMode(Banner.Mode.OFF).sources(HystrixTestClient.class).profiles("client").run(argsList.toArray(new String[0]));

    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer = new PropertySourcesPlaceholderConfigurer();
        propertySourcesPlaceholderConfigurer.setIgnoreUnresolvablePlaceholders(true);
        propertySourcesPlaceholderConfigurer.setOrder(Ordered.LOWEST_PRECEDENCE - 1);
        return propertySourcesPlaceholderConfigurer;

    }

}
