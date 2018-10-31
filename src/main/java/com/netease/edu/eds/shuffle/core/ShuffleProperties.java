package com.netease.edu.eds.shuffle.core;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;

/**
 * *
 * 在spring容器实例化ConfigurationPropertiesBindingPostProcessor.class前的应用场景，是无法通过获取ShuffleProperties来获取值。典型场景就是在postProcessBeanFactory，
 * * * 此时调用，或导致ShuffleProperties对象没有被ConfigurationPropertiesBindingPostProcessor处理。 这些地方改成从Environment中直接获取。
 * 
 * @See ShufflePropertiesSupport.class
 * 安全起见，生命周期不能确定是否晚于ConfigurationPropertiesBindingPostProcessor实例化的，统一直接从Environment中获取。具体参见ShufflePropertiesSupport
 * @author hzfjd
 * @create 18/7/20
 **/
@ConfigurationProperties(prefix = "shuffle")
public class ShuffleProperties {

    /**
     * wait 150ms to send message to latter environment to reduce cross environment consumer compete as less as
     * possible.
     */
    private int                delayMSToSendLatter                  = DELAY_MS_TO_SEND_LATTER;

    public static final long   DEFAULT_TEST_ENV_QUEUE_MESSAGE_TTL   = 1000L * 3600 * 3;

    public static final long   DEFAULT_TEST_ENV_QUEUE_EXPIRE_PERIOD = 1000L * 3600 * 24 * 3;

    public static final int    DELAY_MS_TO_SEND_LATTER              = 100;
    public static final String STANDARD_ENV_NAME                    = "edu-std";
    private List<String>       anonymousTopicNames                  = Arrays.asList("springCloudBus");

    public List<String> getAnonymousTopicNames() {
        return anonymousTopicNames;
    }

    public void setAnonymousTopicNames(List<String> anonymousTopicNames) {
        this.anonymousTopicNames = anonymousTopicNames;
    }

    public int getDelayMSToSendLatter() {
        return delayMSToSendLatter;
    }

    public void setDelayMSToSendLatter(int delayMSToSendLatter) {
        this.delayMSToSendLatter = delayMSToSendLatter;
    }

}
