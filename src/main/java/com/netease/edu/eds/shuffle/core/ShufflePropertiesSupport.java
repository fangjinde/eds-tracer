package com.netease.edu.eds.shuffle.core;

import com.netease.edu.eds.trace.support.SpringBeanFactorySupport;

import java.util.Collections;
import java.util.List;

/**
 * @author hzfjd
 * @create 18/7/20
 **/
public class ShufflePropertiesSupport {

    public static String getStandardEnvName() {
        ShuffleProperties shuffleProperties = SpringBeanFactorySupport.getBean(ShuffleProperties.class);
        if (shuffleProperties == null) {
            return ShuffleProperties.STANDARD_ENV_NAME;
        }
        return shuffleProperties.getStandardEnvName();
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

    public static long getTestEnvQueueExpirePeriod() {
        ShuffleProperties shuffleProperties = SpringBeanFactorySupport.getBean(ShuffleProperties.class);
        if (shuffleProperties == null) {
            return ShuffleProperties.DEFAULT_TEST_ENV_QUEUE_EXPIRE_PERIOD;
        }
        return shuffleProperties.getTestEnvQueueExpirePeriod();
    }

    public static long getTestEnvQueueMessageTtl(){
        ShuffleProperties shuffleProperties = SpringBeanFactorySupport.getBean(ShuffleProperties.class);
        if (shuffleProperties == null) {
            return ShuffleProperties.DEFAULT_TEST_ENV_QUEUE_MESSAGE_TTL;
        }
        return shuffleProperties.getTestEnvQueueMessageTtl();
    }
}
