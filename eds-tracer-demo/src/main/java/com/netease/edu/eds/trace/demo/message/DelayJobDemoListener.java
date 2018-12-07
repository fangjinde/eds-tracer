package com.netease.edu.eds.trace.demo.message;

import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.stereotype.Component;

/**
 * @author hzfjd
 * @create 18/12/5
 **/
@Component("delayJobDemoListener")
public class DelayJobDemoListener implements MessageListener {

    private static Logger logger = LoggerFactory.getLogger(DelayJobDemoListener.class);

    @Override
    public void onMessage(Message message) {

        logger.info("delay message is triggered. with message print below: ");
        logger.info(JSON.toJSONString(message));
        logger.info("message printed. ");
    }
}
