package com.netease.edu.boot.hystrixtest.api.controller;/**
                                                         * Created by hzfjd on 17/12/25.
                                                         */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.netease.edu.boot.hystrix.annotation.EduHystrixCommand;
import com.netease.edu.boot.hystrixtest.dao.EduAttributesDao;
import com.netease.edu.boot.hystrixtest.service.EduAttributesService;
import com.netease.edu.boot.hystrixtest.service.impl.EchoLogic;
import com.netease.edu.web.viewer.ResponseView;

/**
 * @author hzfjd
 * @create 17/12/25
 */
@RestController
@RequestMapping(path = "/api")
public class ApiStratchController {

    @Autowired
    EduAttributesDao     eduAttributesDao;
    @Autowired
    EduAttributesService eduAttributesService;

    @RequestMapping(path = "/getByKey")
    public ResponseView getFromKV(@RequestParam(value = "key", required = false) String key) {

        String value = eduAttributesService.getAttributesValue(key);

        ResponseView responseView = new ResponseView();
        responseView.setResult(value);
        return responseView;

    }

    @RequestMapping(path = "/setValue")
    public ResponseView setValue(@RequestParam(value = "key", required = false) String key,
                                 @RequestParam(value = "value", required = false) String value) {

        boolean updated = eduAttributesService.updateAttributes(key, value);

        ResponseView responseView = new ResponseView();
        responseView.setResult(updated);
        return responseView;

    }

    @Autowired
    EchoLogic echoLogic;

    Logger    logger = LoggerFactory.getLogger(ApiStratchController.class);

    @RequestMapping(path = "/echoWithoutFallback")
    public ResponseView echoWithoutFallback(@RequestParam(value = "testCase", required = false) Integer testCase) {
        return echoLogic.echoOfResponseView(testCase);
    }

    @RequestMapping(path = "/echo")
    @EduHystrixCommand(fallbackMethod = "fallback1")
    public ResponseView echo(@RequestParam(value = "testCase", required = false) Integer testCase,
                             @RequestParam(value = "fallbackDepth", required = false) Integer fallbackDepth) {
        return echoLogic.echoOfResponseView(testCase);
    }

    @EduHystrixCommand(fallbackMethod = "fallback2")
    public ResponseView fallback1(Integer testCase, Integer fallbackDepth, Throwable e) {
        logger.warn("fallback1 on error:", e);
        if (fallbackDepth != null && fallbackDepth.intValue() == 1) {
            ResponseView responseView = new ResponseView();
            responseView.setResult("fallback1");
            return responseView;
        } else {
            throw new RuntimeException("fallback1 can't handle", e);
        }
    }

    public ResponseView fallback2(Integer testCase, Integer fallbackDepth, Throwable e) {
        logger.warn("fallback2 on error:", e);
        if (fallbackDepth != null && fallbackDepth.intValue() == 2) {
            ResponseView responseView = new ResponseView();
            responseView.setResult("fallback2");
            return responseView;
        } else {
            throw new RuntimeException("fallback2 can't handle", e);
        }
    }

}
