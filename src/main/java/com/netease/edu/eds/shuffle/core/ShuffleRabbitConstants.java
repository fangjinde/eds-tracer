package com.netease.edu.eds.shuffle.core;

/**
 * @author hzfjd
 * @create 18/8/24
 **/
public interface ShuffleRabbitConstants {

    String SHUFFLE_DELAY_EXCHANGE      = "shuffle.delay.exchange";
    String SHUFFLE_DELAY_QUEUE         = "shuffle.delay.queue";
    String SHUFFLE_ROUTE_BACK_EXCHANGE = "shuffle.route.back.exchange";

    interface ParamName {

        String X_DEAD_LETTER_EXCHANGE = "x-dead-letter-exchange";
        String X_MESSAGE_TTL          = "x-message-ttl";
    }

}
