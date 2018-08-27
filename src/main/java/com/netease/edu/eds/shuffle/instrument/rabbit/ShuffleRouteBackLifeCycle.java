package com.netease.edu.eds.shuffle.instrument.rabbit;

import com.netease.edu.eds.shuffle.core.EnvironmentShuffleUtils;
import com.netease.edu.eds.shuffle.core.ShufflePropertiesSupport;
import com.netease.edu.eds.shuffle.core.ShuffleRabbitConstants;
import com.netease.edu.eds.shuffle.support.NamedQueueRawNameRegistry;
import com.netease.edu.eds.shuffle.support.QueueShuffleUtils;
import com.netease.edu.eds.shuffle.support.ShuffleEnvironmentInfoProcessUtils;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * 通过“最晚”的lifecycle收集所有binding记录bean（shuffle.route.back.exchange自身的除外，
 * binding的queue为匿名的除外，routekey去除环境信息后不在裸queueNames列表的除外）， 新routekey为裸queueNames， 绑定到对应queue（根据beanFactory中的queue
 * bean的name属性来进行匹配，注意不要使用beanName），更新binding信息到shuffle.route.back.exchange上。
 * 
 * @author hzfjd
 * @create 18/8/24
 **/
public class ShuffleRouteBackLifeCycle implements SmartLifecycle, ApplicationContextAware, BeanFactoryAware {

    private ApplicationContext applicationContext;
    private BeanFactory        beanFactory;
    private boolean            initialized            = false;
    String                     CONNECTION_FACTORY_KEY = "connectionFactoryKey";

    private String getConnectionFactoryKey(RabbitAdmin rabbitAdmin) {

        Map<String, String> connectionFactoryKeyMap = new HashMap<>();

        connectionFactoryKeyMap.put(CONNECTION_FACTORY_KEY, "default");

        ReflectionUtils.doWithFields(RabbitAdmin.class, new ReflectionUtils.FieldCallback() {

            @Override
            public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {
                ReflectionUtils.makeAccessible(field);
                ConnectionFactory connectionFactory = (ConnectionFactory) field.get(rabbitAdmin);
                if (connectionFactory != null) {
                    connectionFactoryKeyMap.put(CONNECTION_FACTORY_KEY,
                                                connectionFactory.getHost() + ":" + connectionFactory.getPort());
                }
            }
        }, (Field field) -> "connectionFactory".equals(field.getName()));

        return connectionFactoryKeyMap.get(CONNECTION_FACTORY_KEY);
    }

    @Override
    public void start() {

        if (initialized) {
            return;
        }

        initialized = true;

        Map<String, RabbitAdmin> rabbitAdminMap = applicationContext.getBeansOfType(RabbitAdmin.class);

        // 根据broker地址去重
        Map<String, RabbitAdmin> connectionFactoryRabbitAdminMap = new HashMap<>();
        for (RabbitAdmin rabbitAdmin : rabbitAdminMap.values()) {
            connectionFactoryRabbitAdminMap.put(getConnectionFactoryKey(rabbitAdmin), rabbitAdmin);
        }

        Map<String, Binding> bindingMap = applicationContext.getBeansOfType(Binding.class);
        if (bindingMap == null) {
            return;
        }
        for (Binding originBinding : bindingMap.values()) {

            if (QueueShuffleUtils.isShuffleInnerExchange(originBinding.getExchange())) {
                continue;
            }

            // 裸queueName做为RouteBackRoutingKey。
            String routeBackRoutingKey = ShuffleEnvironmentInfoProcessUtils.getRawNameWithoutCurrentEnvironmentInfo(originBinding.getDestination());

            // 注册表里面的queue都是非匿名queue。因此不用额外查询所有Queue Bean进行过滤。
            if (!NamedQueueRawNameRegistry.contain(routeBackRoutingKey)) {
                continue;
            }

            // 替换为对应std环境的queueName
            String relatedStdQueueName = originBinding.getDestination();
            if (!ShufflePropertiesSupport.getStandardEnvName().equals(EnvironmentShuffleUtils.getCurrentEnv())) {
                relatedStdQueueName = ShuffleEnvironmentInfoProcessUtils.getNameWithNewFixOrRemoveOldFix(originBinding.getDestination(),
                                                                                                         EnvironmentShuffleUtils.getCurrentEnv(),
                                                                                                         ShufflePropertiesSupport.getStandardEnvName());
            }

            Binding routeBackBinding = new Binding(relatedStdQueueName, originBinding.getDestinationType(),
                                                   ShuffleRabbitConstants.SHUFFLE_ROUTE_BACK_EXCHANGE,
                                                   routeBackRoutingKey, null);

            if (beanFactory instanceof ConfigurableListableBeanFactory) {
                ConfigurableListableBeanFactory configurableListableBeanFactory = (ConfigurableListableBeanFactory) beanFactory;
                String bindingBeanName = ShuffleRabbitConstants.SHUFFLE_ROUTE_BACK_EXCHANGE + "." + routeBackRoutingKey;
                if (!configurableListableBeanFactory.containsBean(bindingBeanName)) {
                    configurableListableBeanFactory.registerSingleton(bindingBeanName, routeBackBinding);
                }

            }

            for (RabbitAdmin rabbitAdmin : connectionFactoryRabbitAdminMap.values()) {
                rabbitAdmin.declareBinding(routeBackBinding);
            }

        }

    }

    @Override
    public void stop() {

    }

    @Override
    public boolean isRunning() {
        return initialized;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE - 2;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public void stop(Runnable callback) {

    }
}
