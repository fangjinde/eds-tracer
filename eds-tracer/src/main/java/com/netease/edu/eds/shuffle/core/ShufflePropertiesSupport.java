package com.netease.edu.eds.shuffle.core;

import com.netease.edu.eds.trace.support.SpringBeanFactorySupport;
import org.apache.commons.lang.StringUtils;
import org.springframework.core.env.Environment;

import java.util.Collections;
import java.util.List;

/**
 * 在spring容器实例化ConfigurationPropertiesBindingPostProcessor.class前的应用场景，是无法通过获取ShuffleProperties来获取值。典型场景就是在postProcessBeanFactory，
 * * 此时调用，或导致ShuffleProperties对象没有被ConfigurationPropertiesBindingPostProcessor处理。 这些地方改成从Environment中直接获取。
 * 
 * @author hzfjd
 * @create 18/7/20
 **/
public class ShufflePropertiesSupport {

    /**
     * switch
     */
    public static boolean turnOn() {
        Environment environment = SpringBeanFactorySupport.getEnvironment();
        String turnOnValue = null;
        if (environment != null) {
            turnOnValue = environment.getProperty("shuffle.turnOn");
        }

        if (StringUtils.isNotBlank(turnOnValue)) {
            return Boolean.parseBoolean(turnOnValue);
        }

        return false;
    }

    /**
     * standard env name, default is edu-std.
     */
    public static String getStandardEnvName() {

        Environment environment = SpringBeanFactorySupport.getEnvironment();
        String standardEnvName = null;
        if (environment != null) {
            standardEnvName = environment.getProperty("shuffle.standardEnvName");
        }

        if (StringUtils.isNotBlank(standardEnvName)) {
            return standardEnvName;
        }

        return ShuffleProperties.STANDARD_ENV_NAME;

    }

    public static int getDelayMSToSendLatter() {
        ShuffleProperties shuffleProperties = SpringBeanFactorySupport.getBean(ShuffleProperties.class);
        if (shuffleProperties == null) {
            return ShuffleProperties.DELAY_MS_TO_SEND_LATTER;
        }
        return shuffleProperties.getDelayMSToSendLatter();
    }

    public static List<String> getAnonymousTopicNames() {
        ShuffleProperties shuffleProperties = SpringBeanFactorySupport.getBean(ShuffleProperties.class);
        if (shuffleProperties == null) {
            return Collections.emptyList();
        }
        return shuffleProperties.getAnonymousTopicNames();
    }

    /**
     * queue expire period in test env. default is 3d.
     */
    public static long getTestEnvQueueExpirePeriod() {
        Environment environment = SpringBeanFactorySupport.getEnvironment();
        String testEnvQueueExpirePeriodValue = null;
        if (environment != null) {
            testEnvQueueExpirePeriodValue = environment.getProperty("shuffle.testEnvQueueExpirePeriod");
        }

        if (StringUtils.isNotBlank(testEnvQueueExpirePeriodValue)) {
            return Long.parseLong(testEnvQueueExpirePeriodValue);
        }

        return ShuffleProperties.DEFAULT_TEST_ENV_QUEUE_EXPIRE_PERIOD;

    }

    /**
     * queue message ttl in test env. default is 3h.
     */
    public static long getTestEnvQueueMessageTtl() {
        Environment environment = SpringBeanFactorySupport.getEnvironment();
        String testEnvQueueMessageTtlValue = null;
        if (environment != null) {
            testEnvQueueMessageTtlValue = environment.getProperty("shuffle.testEnvQueueMessageTtl");
        }

        if (StringUtils.isNotBlank(testEnvQueueMessageTtlValue)) {
            return Long.parseLong(testEnvQueueMessageTtlValue);
        }

        return ShuffleProperties.DEFAULT_TEST_ENV_QUEUE_MESSAGE_TTL;
    }

}
