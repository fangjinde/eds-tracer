package com.netease.edu.eds.shuffle.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.netease.edu.eds.shuffle.server.service.ShuffleService;
import com.netease.edu.eds.shuffle.spi.KeyValueManager;

/**
 * @author hzfjd
 * @create 18/12/19
 **/
public class DubboKeyValueManager implements KeyValueManager {

    private static final Logger logger = LoggerFactory.getLogger(DubboKeyValueManager.class);

    @Autowired
    private ShuffleService      shuffleService;

    @Override
    public String getValue(String key) {

        try {
            return shuffleService.getValue(key);
        } catch (Exception e) {
            logger.error(String.format("getValue error, with key=%s", key), e);
            return null;
        }

    }

    @Override
    public void setValue(String key, String value, Integer expireInSeconds) {
        try {
            shuffleService.setValue(key, value, expireInSeconds);
        } catch (Exception e) {
            logger.error(String.format("setValue error, with key=%s, value=%s", key, value), e);
        }
    }

    @Override
    public Long increment(String key, long delta, Integer expireInSeconds) {
        try {

            return shuffleService.increment(key, delta, expireInSeconds);
        } catch (Exception e) {
            logger.error(String.format("incrementValue error, with key=%s, delta=%s", key, delta), e);
            return 0L;
        }
    }
}
