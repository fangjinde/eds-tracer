package com.netease.edu.boot.hystrixtest.service.impl;

import com.netease.edu.boot.hystrixtest.constants.TransactionMessageTestContants;
import com.netease.edu.boot.hystrixtest.service.EduAttributesService;
import com.netease.edu.boot.hystrixtest.service.TransactionMessageTestService;
import com.netease.edu.transaction.message.client.annotation.TransactionMessage;
import com.netease.edu.transaction.message.client.client.TransactionMessageClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @author hzfjd
 * @create 18/7/4
 **/
@Component
public class TransactionMessageTestServiceImpl implements TransactionMessageTestService {

    @Autowired
    private EduAttributesService     eduAttributesService;
    @Autowired
    private TransactionMessageClient transactionMessageClient;

    @Override
    @TransactionMessage
    public void interProcessTransactionalBiz() {

        String curTime = String.valueOf(System.currentTimeMillis());
        eduAttributesService.updateAttributes(TransactionMessageTestContants.Key1, curTime);

        Map event = new HashMap<String, Object>();
        event.put("someKey", "someValue");
        event.put("curTime", curTime);
        transactionMessageClient.prepare(TransactionMessageTestContants.ROUTING_KEY, event);

    }
}
