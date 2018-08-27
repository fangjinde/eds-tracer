package com.netease.edu.eds.shuffle.instrument.rabbit;

import com.netease.edu.eds.shuffle.core.ShufflePropertiesSupport;
import com.netease.edu.eds.shuffle.support.QueueShuffleUtils;
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
import java.util.List;

/**
 * 1.兼容历史上，通过xml创建的没有带环境前、后缀的队列或交换机名称。 注意：注解模式和Spring Cloud Stream Rabbit需要从编码层面加上环境后缀。主要是有些匿名队列、Declarable 的Bean
 * Register 和RabbitAdmin的declare的先后顺序，触发时机都有差别，处理起来有很多细节。所以暂不做实现。事实上，后两种目前线上采用的还比较少，可以从编码规范上解决。
 * 
 * @author hzfjd
 * @create 18/8/14
 **/
public class SpringRabbitComponentNameEnvironmentCustomBeanFactoryPostProcessor implements BeanDefinitionRegistryPostProcessor {

    private static final String       LISTENER_CONTAINER_FACTORY_BEAN_CLASS_NAME = "org.springframework.amqp.rabbit.config.ListenerContainerFactoryBean";

    private static final String[]     rabbitEnvironmentKeyArray                  = { "${local_service_version_suffix}",
                                                                                     "${spring.profiles.active}" };
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

            // shuffle内部队列或交换机不要做环境后缀
            if (QueueShuffleUtils.isShuffleInnerQueueOrExchange(value)) {
                return;
            }

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
