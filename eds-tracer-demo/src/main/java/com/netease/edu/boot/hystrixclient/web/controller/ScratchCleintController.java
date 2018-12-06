package com.netease.edu.boot.hystrixclient.web.controller;/**
 * Created by hzfjd on 18/1/16.
 */

import com.netease.edu.boot.hystrix.annotation.EduHystrixCommand;
import com.netease.edu.boot.hystrixtest.service.HystrixTestWithFallbackService;
import com.netease.edu.util.exceptions.FrontNotifiableRuntimeException;
import com.netease.edu.util.exceptions.SystemErrorRuntimeException;
import com.netease.edu.web.viewer.ResponseView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

/**
 * @author hzfjd
 * @create 18/1/16
 */
@RestController
@RequestMapping(path = "/web")
public class ScratchCleintController {

    Logger logger = LoggerFactory.getLogger(ScratchCleintController.class);

    @Autowired
    HystrixTestWithFallbackService hystrixTestWithFallbackService;

    @Autowired
    public void setRestTemplateBuilder(RestTemplateBuilder restTemplateBuilder) {
        restTemplate = restTemplateBuilder.rootUri("http://127.0.0.1:20022/web").build();
    }

    RestTemplate restTemplate;

    @RequestMapping(path = "/echoNoTrace")
    @EduHystrixCommand(fallbackMethod = "fallback1")
    public ResponseView echoNoTrace(@RequestParam(value = "testCase", required = false) Integer testCase,
                             @RequestParam(value = "fallbackDepth", required = false) Integer fallbackDepth) {


        String echoDubbo = hystrixTestWithFallbackService.echoWithFallbackSupport(testCase);

        ResponseView responseView = restTemplate.getForObject("/echo?testCase={testCase}",
                                                              ResponseView.class, testCase);

        return echoOfResponseView(testCase, echoDubbo, responseView.getResult().toString());
    }

    @RequestMapping(path = "/echo")
    @EduHystrixCommand(fallbackMethod = "fallback1")
    public ResponseView echo(@RequestParam(value = "testCase", required = false) Integer testCase,
                             @RequestParam(value = "fallbackDepth", required = false) Integer fallbackDepth) {


        String echoDubbo = hystrixTestWithFallbackService.echoWithFallbackSupport(testCase);

        ResponseView responseView = restTemplate.getForObject("/echo?testCase={testCase}",
                                                              ResponseView.class, testCase);

        return echoOfResponseView(testCase, echoDubbo, responseView.getResult().toString());
    }

    public ResponseView echoOfResponseView(Integer testCase, String... echos) {
        if (testCase == null) {
            return new ResponseView();
        } else if (testCase == 1) {
            return new ResponseView(ResponseView.OK_NOT_LOGIN, "user bad request error");
        } else if (testCase == 2) {
            return new ResponseView(ResponseView.EXCEPTION_IGNORE, "system error");
        } else if (testCase == 11) {
            throw new FrontNotifiableRuntimeException("i should be ignored by hystrix, and pass to downstream");
        } else if (testCase == 12) {
            throw new SystemErrorRuntimeException("i should be record by hystrix, and trigger fallback if it exists");
        }
        ResponseView responseView = new ResponseView();
        responseView.setResult(testCase.toString() + echos);
        return responseView;
    }


    public ResponseView fallback1(Integer testCase, Integer fallbackDepth, Throwable e) {
        logger.warn("fallback1 on error:", e);
        if (fallbackDepth !=null && fallbackDepth.intValue() == 1) {
            ResponseView responseView = new ResponseView();
            responseView.setResult("fallback1");
            return responseView;
        } else {
            throw new RuntimeException("fallback1 can't handle", e);
        }
    }
}
