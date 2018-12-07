package com.netease.edu.eds.trace.demo.service.impl;

import com.netease.edu.eds.trace.demo.service.AsyncTestService;
import com.netease.edu.eds.trace.demo.service.EduAttributesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * @author hzfjd
 * @create 18/6/26
 **/
@Component
public class AsyncTestServiceImpl implements AsyncTestService {

    private static final Logger               logger = LoggerFactory.getLogger(AsyncTestServiceImpl.class);
    @Autowired
    private              EduAttributesService eduAttributesService;
    @Override
    @Async
    public void asyncDo() {
        logger.info("async do");
        String key = "ddb_trace_demo_ops_key";
        boolean updated = eduAttributesService.updateAttributes(key, String.valueOf(new Random().nextLong()));
    }
}
