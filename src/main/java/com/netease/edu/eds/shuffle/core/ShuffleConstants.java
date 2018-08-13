package com.netease.edu.eds.shuffle.core;

/**
 * @author hzfjd
 * @create 18/8/6
 **/
public interface ShuffleConstants {

    /**
     * 3days
     */
    int    DUPLICATE_CHECK_VALID_PERIOD = 3 * 24 * 3600;

    /**
     * 回游exchange
     */
    String SHUFFLE_ROUTE_BACK_EXCHANGE  = "shuffle.route.back.exchange";

}
