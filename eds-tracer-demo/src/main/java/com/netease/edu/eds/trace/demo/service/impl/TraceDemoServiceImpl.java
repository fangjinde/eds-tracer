package com.netease.edu.eds.trace.demo.service.impl;

import com.netease.edu.eds.trace.democommon.service.TraceDemoDownstreamService;
import com.netease.edu.eds.trace.democommon.service.TraceDemoService;
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
