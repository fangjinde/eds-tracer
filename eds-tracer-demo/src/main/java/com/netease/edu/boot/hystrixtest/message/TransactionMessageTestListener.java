package com.netease.edu.boot.hystrixtest.message;

import com.netease.edu.boot.hystrixtest.constants.TransactionMessageTestContants;
import com.netease.edu.boot.hystrixtest.service.EduAttributesService;
import com.netease.edu.persist.transaction.annotation.EduTransaction;
import com.netease.edu.transaction.message.share.constants.TransactionMessageConstants;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author hzfjd
 * @create 18/7/4
 **/
@Component
public class TransactionMessageTestListener {

    @Autowired
    private EduAttributesService eduAttributesService;

    @EduTransaction
    @RabbitListener(containerFactory = TransactionMessageConstants.Bean.rabbitListenerContainerFactory, bindings = { @QueueBinding(key = "${spring.profiles.active}_"
                                                                                                                                         + TransactionMessageTestContants.ROUTING_KEY, value = @Queue(value = "${spring.profiles.active}_"
                                                                                                                                                                                                              + TransactionMessageTestContants.ROUTING_KEY
                                                                                                                                                                                                              + "_testListener", durable = "true"), exchange = @Exchange(type = ExchangeTypes.TOPIC, value = TransactionMessageConstants.Exchange.TOPIC, durable = "true")) })

    public void onMessage(Map<String, Object> event) {

        String curTime = (String) event.get("curTime");
        if (curTime != null) {
            System.out.println(curTime);
        }
        eduAttributesService.updateAttributes(TransactionMessageTestContants.Key2, curTime);

    }
}
