package com.netease.edu.boot.hystrixtest.api.controller;/**
 * Created by hzfjd on 17/12/25.
 */

import com.netease.edu.boot.hystrix.annotation.EduHystrixCommand;
import com.netease.edu.boot.hystrixtest.service.impl.EchoLogic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * @author hzfjd
 * @create 17/12/25
 */
@RestController
public class ApiStratchController {

    @Autowired
    EchoLogic echoLogic;

    Logger logger = LoggerFactory.getLogger(ApiStratchController.class);

    @RequestMapping(path = "/echoWithoutFallback")
    public String echoWithoutFallback(@RequestParam(value = "testCase", required = false) Integer testCase) {
        return echoLogic.innerEcho(testCase);
    }


    @RequestMapping(path = "/echo")
    @EduHystrixCommand(fallbackMethod = "fallback1")
    public String echo(@RequestParam(value = "testCase", required = false) Integer testCase) {
      return echoLogic.innerEcho(testCase);
    }

    @EduHystrixCommand(fallbackMethod = "fallback2")
    public String fallback1(Integer testCase, Throwable e) {
        logger.warn("fallback on error:", e);
        if (testCase != null && testCase >= 2) {
            throw new RuntimeException("doubleFallback1 error", e);
        }

        return "doubleFallback1 ok";
    }

    public String fallback2(Integer error, Throwable e) {
        logger.warn("fallback on error:", e);
        if (error != null && error >= 3) {
            throw new RuntimeException("doubleFallback1 error", e);
        }

        return "doubleFallback2 ok";
    }

    private static Map<String, String> data = new HashMap<String, String>(3);

    static {
        data.put("1", "a");
        data.put("2", "t");
        data.put("3", "b");
        data.put("4", "j");
    }


}
