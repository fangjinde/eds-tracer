package com.netease.edu.eds.shuffle.server.service;

/**
 * @author hzfjd
 * @create 18/12/19
 **/
public interface ShuffleService {

    String getValue(String key);

    void setValue(String key, String value, Integer expireInSeconds);

    Long increment(String key, Long delta, Integer expireInSeconds);

}
