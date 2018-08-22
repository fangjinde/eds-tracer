package com.netease.edu.eds.shuffle.instrument.rabbit;

import com.netease.edu.eds.shuffle.core.ShufflePropertiesSupport;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.core.Conventions;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author hzfjd
 * @create 18/8/14
 **/
@Deprecated
public class SpringRabbitShuffleCustomBeanFactoryPostProcessor implements BeanDefinitionRegistryPostProcessor {

    private static final String       LISTENER_CONTAINER_FACTORY_BEAN_CLASS_NAME = "org.springframework.amqp.rabbit.config.ListenerContainerFactoryBean";

    private static final String[]     rabbitEnvironmentKeyArray                  = { "${local_service_version_suffix}" };
    private static final List<String> rabbitEnvironmentKeyList                   = Arrays.asList(rabbitEnvironmentKeyArray);

    private static final String       rabbitEnvironmentDefaultSuffix             = "${local_service_version_suffix}";

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        for (String beanName : registry.getBeanDefinitionNames()) {
            BeanDefinition beanDefinition = registry.getBeanDefinition(beanName);
            if (TopicExchange.class.getName().equals(beanDefinition.getBeanClassName())) {
                processExchange(beanName, beanDefinition);
            } else if (FanoutExchange.class.getName().equals(beanDefinition.getBeanClassName())) {
                processExchange(beanName, beanDefinition);
            } else if (DirectExchange.class.getName().equals(beanDefinition.getBeanClassName())) {
                processExchange(beanName, beanDefinition);
            } else if (Queue.class.getName().equals(beanDefinition.getBeanClassName())) {
                // AnonymousQueue will be ignored
                processQueue(beanName, beanDefinition);
            } else if (SimpleMessageListenerContainer.class.getName().equals(beanDefinition.getBeanClassName())) {
                processListenContainer(beanName, beanDefinition);
            } else if (LISTENER_CONTAINER_FACTORY_BEAN_CLASS_NAME.equals(beanDefinition.getBeanClassName())) {
                // compatible for spring rabbit 2.0 and above
                processListenContainer(beanName, beanDefinition);
            } else if (RabbitTemplate.class.getName().equals(beanDefinition.getBeanClassName())) {
                processRabbitTempate(beanName, beanDefinition);
            }

        }
    }

    private void processRabbitTempate(String beanName, BeanDefinition beanDefinition) {

        String propertyName = Conventions.attributeNameToPropertyName("exchange");
        TypedStringValue propertyTypedStringValue = (TypedStringValue) beanDefinition.getPropertyValues().get(propertyName);
        changeStringValueIfNotExistedEnvironmentInfo(propertyTypedStringValue);

    }

    /**
     * 若原属性或构造器参数没有包含环境信息，则统一增加环境后缀。
     * 
     * @param typedStringValue
     */
    private void changeStringValueIfNotExistedEnvironmentInfo(TypedStringValue typedStringValue) {
        if (typedStringValue != null && typedStringValue.getValue() != null) {
            String value = typedStringValue.getValue();
            boolean contain = false;
            for (String rabbitEnviromentKey : rabbitEnvironmentKeyList) {
                if (value != null && value.contains(rabbitEnviromentKey)) {
                    contain = true;
                    break;
                }
            }

            // 不包含则增加后缀
            if (!contain) {
                value = value + rabbitEnvironmentDefaultSuffix;
                typedStringValue.setValue(value);
            }
        }
    }

    private String getStringValueWithoutEnvironmentInfo(TypedStringValue typedStringValue) {
        if (typedStringValue != null && typedStringValue.getValue() != null) {
            String value = typedStringValue.getValue();
            for (String rabbitEnviromentKey : rabbitEnvironmentKeyList) {
                if (value != null && value.contains(rabbitEnviromentKey)) {
                    value = value.replaceAll(rabbitEnviromentKey, "");
                }
            }
            return value;

        }
        return null;
    }

    /**
     * 绑定queues的场景不需要考虑，因此是通过RuntimeBeanReference进行引用。
     * 
     * @param beanName
     * @param beanDefinition
     */
    private void processListenContainer(String beanName, BeanDefinition beanDefinition) {
        String propertyName = "queueNames";
        List<TypedStringValue> propertyTypedStringValueList = (List<TypedStringValue>) beanDefinition.getPropertyValues().get(propertyName);
        if (CollectionUtils.isNotEmpty(propertyTypedStringValueList)) {
            for (TypedStringValue typedStringValue : propertyTypedStringValueList) {
                changeStringValueIfNotExistedEnvironmentInfo(typedStringValue);
            }
        }
    }

    private void processQueue(String beanName, BeanDefinition beanDefinition) {
        addEnvironmentKeyIfNotExisted(beanDefinition);
        addQueueArgumentsForShuffleIfNotExistd(beanDefinition);

    }

    /**
     * 为队列增加增加ttl、dle、expire等设置，主要是测试环境销毁后的消息转移，消息队列过期删除，基准环境的消息延迟处理等。
     * 已经有QueueArguments的需要排除。前者往往是业务中延迟队列，和shuffle机制正常情况下可以完美兼容。
     *
     * @param beanDefinition
     */
    private void addQueueArgumentsForShuffleIfNotExistd(BeanDefinition beanDefinition) {

        ConstructorArgumentValues constructorArgumentValues = beanDefinition.getConstructorArgumentValues();
        // 异常情况，不做额外处理
        if (constructorArgumentValues == null || constructorArgumentValues.getArgumentCount() < 4) {
            return;
        }

        // 含有QueueArguments的，为5参数构造函数
        if (constructorArgumentValues.getArgumentCount() >= 5) {
            return;
        }

        TypedStringValue queueNameTypeStringValue = (TypedStringValue) constructorArgumentValues.getIndexedArgumentValue(0,
                                                                                                                         String.class).getValue();

        // <rabbit:queue-arguments>

        // <entry key="x-dead-letter-exchange" value="shuffle.delay.exchange"/>
        // <!-- 只能路由回std环境的对应队列-->
        // <entry key="x-dead-letter-routing-key" value="shuffle.dlq.routing.key.shuffleDemoQueueC1-std"/>
        // <!-- 测试环境堆积一天以上的消息路由给std处理-->
        // <entry key="x-message-ttl" value="1800000" value-type="java.lang.Long"/>
        // <!-- 三天后销毁无用队列-->
        // <entry key="x-expires" value="3600000" value-type="java.lang.Long"/>
        //
        // </rabbit:queue-arguments>

        Map<TypedStringValue, TypedStringValue> arguments = new HashMap<>();
        arguments.put(new TypedStringValue("x-dead-letter-exchange"), new TypedStringValue("shuffle.delay.exchange"));
        arguments.put(new TypedStringValue("x-dead-letter-routing-key"),
                      new TypedStringValue(getStdDeadLetterQueueRoutingKey(queueNameTypeStringValue)));
        arguments.put(new TypedStringValue("x-message-ttl"), new TypedStringValue("1800000", Long.class));
        arguments.put(new TypedStringValue("x-expires"), new TypedStringValue("3600000", Long.class));
        constructorArgumentValues.addIndexedArgumentValue(5, arguments);

    }

    private String getStdDeadLetterQueueRoutingKey(TypedStringValue queueNameTypeStringValue) {
        String queueNameWithoutEnvironmentInfo = getStringValueWithoutEnvironmentInfo(queueNameTypeStringValue);
        return "shuffle.dlq.routing.key." + queueNameWithoutEnvironmentInfo + "."
               + ShufflePropertiesSupport.getStandardEnvName();
    }

    private void addEnvironmentKeyIfNotExisted(BeanDefinition beanDefinition) {
        ConstructorArgumentValues constructorArgumentValues = beanDefinition.getConstructorArgumentValues();
        if (constructorArgumentValues != null && constructorArgumentValues.getArgumentCount() > 0) {
            TypedStringValue exchangeTypedStringValue = (TypedStringValue) constructorArgumentValues.getArgumentValue(0,
                                                                                                                      String.class).getValue();
            changeStringValueIfNotExistedEnvironmentInfo(exchangeTypedStringValue);
        }
    }

    private void processExchange(String beanName, BeanDefinition beanDefinition) {

        addEnvironmentKeyIfNotExisted(beanDefinition);
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

    }
}
