package com.netease.edu.eds.trace.demo;/**
                                        * Created by hzfjd on 18/1/8.
                                        */

import com.netease.edu.eds.trace.demo.aop.TransactionAspect;
import com.netease.edu.eds.trace.demo.constants.ApplicationCommandArgs;
import com.netease.edu.eds.trace.demo.ioc.ExcludeBeanFactoryPostProcessor;
import com.netease.edu.eds.trace.demo.message.stream.binding.ShuffleStreamBinding;
import com.netease.edu.transaction.message.client.config.TransactionMessageClientConfig;
import org.apache.commons.collections.CollectionUtils;
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
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
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
                                     EmbeddedMongoAutoConfiguration.class, RabbitAutoConfiguration.class,
                                     SpringDataWebAutoConfiguration.class })
@EnableFeignClients
@EnableDiscoveryClient
@ImportResource({ "classpath:applicationContext-server.xml" })
// @EnableAsync(proxyTargetClass = true)
@Import(value = { TransactionMessageClientConfig.class })
@EnableBinding({ ShuffleStreamBinding.class })
public class TraceDemoApplication {

    public static void main(String[] args) {

        String[] argsDiff = { "--spring.application.name=eds-tracer-demo" };
        List<String> argsList = new ArrayList<>(args.length + ApplicationCommandArgs.SAME_ARGS.length
                                                + argsDiff.length);
        CollectionUtils.addAll(argsList, args);
        CollectionUtils.addAll(argsList, ApplicationCommandArgs.SAME_ARGS);
        CollectionUtils.addAll(argsList, argsDiff);

        SpringApplication.run(TraceDemoApplication.class, argsList.toArray(new String[0]));
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer = new PropertySourcesPlaceholderConfigurer();
        propertySourcesPlaceholderConfigurer.setIgnoreUnresolvablePlaceholders(true);
        propertySourcesPlaceholderConfigurer.setOrder(Ordered.LOWEST_PRECEDENCE - 1);
        return propertySourcesPlaceholderConfigurer;

    }

    @Bean
    public static ExcludeBeanFactoryPostProcessor excludeBeanFactoryPostProcessor() {
        return new ExcludeBeanFactoryPostProcessor();
    }

    @Bean
    public TransactionAspect TransactionAspect() {
        return new TransactionAspect();
    }

}
