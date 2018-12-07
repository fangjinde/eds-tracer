package com.netease.edu.eds.trace.demo.message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.netease.edu.util.jms.ExponentialBackoffAmqpRetryMessageListener;

import javax.annotation.Resource;

/**
 * @author hzfjd
 * @create 18/8/14
 **/
@Component("shuffleRetryableDemoListener")
public class ShuffleRetryableDemoListener extends ExponentialBackoffAmqpRetryMessageListener {

    private static Logger logger = LoggerFactory.getLogger(ShuffleRetryableDemoListener.class);


    @Resource(name="shuffleRetryForwardAmqpTemplate")
    private AmqpTemplate shuffleRetryForwardAmqpTemplate;

    @Override
    public AmqpTemplate getRetryAmqpTemplate() {
        return shuffleRetryForwardAmqpTemplate;
    }

    @Override
    public void onIdempotentConsuming(Message message) {
        logger.info("consuming:" + JSON.toJSONString(message));
        throw new RuntimeException("exception for retry");

    }
}
