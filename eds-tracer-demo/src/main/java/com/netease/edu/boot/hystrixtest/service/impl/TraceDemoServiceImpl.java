package com.netease.edu.boot.hystrixtest.service.impl;

import com.netease.edu.boot.hystrixclient.service.TraceDemoDownstreamService;
import com.netease.edu.boot.hystrixtest.service.TraceDemoService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author hzfjd
 * @create 18/7/18
 **/
@Component
public class TraceDemoServiceImpl implements TraceDemoService {

    @Resource
    private TraceDemoDownstreamService traceDemoDownstreamService;

    @Override
    public String doSth(String args) {
        return this.getClass().getSimpleName() + traceDemoDownstreamService.doSth(args);
    }

    @Override
    public String fail() {
        try {
            traceDemoDownstreamService.fail();
        } catch (RuntimeException e) {
            throw new RuntimeException(this.getClass().getSimpleName() + "fail as u wished", e);
        }

        throw new RuntimeException(this.getClass().getSimpleName() + "fail as u wished");

    }
}
