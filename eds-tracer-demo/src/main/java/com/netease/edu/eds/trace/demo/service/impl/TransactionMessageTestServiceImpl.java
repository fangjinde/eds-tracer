package com.netease.edu.eds.trace.demo.service.impl;

import com.netease.edu.eds.trace.demo.constants.TransactionMessageTestContants;
import com.netease.edu.eds.trace.demo.service.EduAttributesService;
import com.netease.edu.eds.trace.demo.service.TransactionMessageTestService;
import com.netease.edu.persist.transaction.annotation.EduTransaction;
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
    @EduTransaction
    public void interProcessTransactionalBiz() {

        String curTime = String.valueOf(System.currentTimeMillis());
        eduAttributesService.updateAttributes(TransactionMessageTestContants.Key1, curTime);

        Map event = new HashMap<String, Object>();
        event.put("someKey", "someValue");
        event.put("curTime", curTime);
        transactionMessageClient.prepare(TransactionMessageTestContants.ROUTING_KEY, event);

    }
}
