package com.netease.edu.eds.shuffle.support;

import com.alibaba.fastjson.JSON;
import com.netease.edu.eds.shuffle.core.EnvironmentShuffleUtils;
import com.netease.edu.eds.shuffle.core.ShufflePropertiesSupport;
import com.netease.edu.eds.shuffle.core.ShuffleRabbitConstants;
import com.netease.edu.eds.shuffle.core.ShuffleSwitch;
import org.apache.commons.collections.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Queue;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * @author hzfjd
 * @create 18/8/23
 **/
public class QueueShuffleUtils {

    private static final Logger logger                    = LoggerFactory.getLogger(QueueShuffleUtils.class);

    private static String       RABBIT_INNER_QUEUE_PREFIX = "amq.";

    public static boolean isShuffleInnerQueueOrExchange(String declarableName) {
        if (ShuffleRabbitConstants.SHUFFLE_ROUTE_BACK_EXCHANGE.equals(declarableName)
            || ShuffleRabbitConstants.SHUFFLE_DELAY_EXCHANGE.equals(declarableName)
            || ShuffleRabbitConstants.SHUFFLE_DELAY_QUEUE.equals(declarableName)) {
            return true;
        }
        return false;
    }

    public static boolean isShuffleInnerQueue(String declarableName) {
        if (ShuffleRabbitConstants.SHUFFLE_DELAY_QUEUE.equals(declarableName)) {
            return true;
        }
        return false;
    }

    /**
     * shuffle开关打开时，对queue的参数进行定制化
     * 
     * @param queue
     */
    public static void addQueueArgumentsForShuffle(Queue queue) {

        if (!ShuffleSwitch.isTurnOn()) {
            return;
        }

        if (queue.isExclusive()) {
            // 匿名queue（近似判断）不做处理。
            return;
        }

        // 系统queue不做处理
        if (queue.getName().startsWith(RABBIT_INNER_QUEUE_PREFIX)) {
            return;
        }

        if (isShuffleInnerQueue(queue.getName())) {
            return;
        }

        // 收集所有环境的queue name。需要处理掉环境前、后缀。
        String rawQueueName = ShuffleEnvironmentInfoProcessUtils.getRawNameWithoutCurrentEnvironmentInfo(queue.getName());
        NamedQueueRawNameRegistry.add(rawQueueName);

        // 只有测试环境需要对queue有参数定制需求
        if (ShufflePropertiesSupport.getStandardEnvName().equals(EnvironmentShuffleUtils.getCurrentEnv())) {
            return;
        }

        Map<String, Object> queueArguments = queue.getArguments();
        if (MapUtils.isNotEmpty(queueArguments)) {
            return;
        }

        if (queueArguments == null) {
            queueArguments = new HashMap<>();
        }

        queueArguments.put("x-dead-letter-exchange", "shuffle.delay.exchange");
        queueArguments.put("x-dead-letter-routing-key", rawQueueName);
        queueArguments.put("x-message-ttl", 1800000L);
        queueArguments.put("x-expires", 3600000L);

        try {
            setQueueArguments(queue, queueArguments);
        } catch (Exception e) {
            logger.error("fail to set queueArguments of Queue:" + JSON.toJSONString(queue) + " queueArguments:"
                         + JSON.toJSONString(queueArguments), e);
        }
    }

    public static void setQueueArguments(Queue queue, Map<String, Object> queueArguments) {

        ReflectionUtils.doWithFields(Queue.class, new ReflectionUtils.FieldCallback() {

            @Override
            public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {
                ReflectionUtils.makeAccessible(field);
                field.set(queue, queueArguments);
            }
        }, (Field field) -> "arguments".equals(field.getName()));
    }

    public static void main(String[] args) {
        Queue queue = new Queue("test");

        Map<String, Object> queueArguments = new HashMap<>();
        queueArguments.put("x-dead-letter-exchange", "shuffle.delay.exchange");
        queueArguments.put("x-dead-letter-routing-key", "rawQueueName");
        queueArguments.put("x-message-ttl", 1800000L);
        queueArguments.put("x-expires", 3600000L);

        setQueueArguments(queue, queueArguments);

        System.out.println(JSON.toJSONString(queue));
    }
}
