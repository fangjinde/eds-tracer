package com.netease.edu.boot.hystrixtest.web.controller;/**
 * Created by hzfjd on 18/1/16.
 */

import com.netease.edu.boot.hystrixtest.service.impl.EchoLogic;
import com.netease.edu.web.viewer.ResponseView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author hzfjd
 * @create 18/1/16
 */
@RestController
public class MemberController {

    @Autowired
    EchoLogic echoLogic;

    Logger logger = LoggerFactory.getLogger(MemberController.class);

    @RequestMapping(path = "/health/status")
    public ResponseView echoWithoutFallback(@RequestParam(value = "testCase", required = false) Integer testCase) {
        return echoLogic.echoOfResponseView(testCase);
    }

    public static void main(String[] args) {
        System.out.println(1 << 2);
    }

}
