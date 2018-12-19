package com.netease.edu.eds.trace.demo.message;/**
 * Created by hzfjd on 18/4/13.
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;

/**
 * @author hzfjd
 * @create 18/4/13
 */
@Component("traceDemoMessageListener")
public class TraceDemoMessageListener implements MessageListener {

    private Logger logger = LoggerFactory.getLogger(TraceDemoMessageListener.class);

    @Override public void onMessage(Message message) {
        String body = new String(message.getBody(), Charset.forName("utf-8"));
        logger.info(body);
        logger.info(message.getMessageProperties().toString());
    }
}
