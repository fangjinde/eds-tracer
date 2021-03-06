package com.netease.edu.eds.trace.clientdemo.service.impl;

import org.springframework.stereotype.Component;

import com.netease.edu.eds.trace.democommon.service.TraceDemoDownstreamService;

/**
 * @author hzfjd
 * @create 18/7/25
 **/
@Component
public class TraceDemoDownstreamServiceImpl implements TraceDemoDownstreamService {

    @Override
    public String doSth(String args) {
        return this.getClass().getSimpleName() + args;

    }

    @Override
    public String fail() {
        throw new RuntimeException(this.getClass().getSimpleName() + "fail as u wished");
    }
}
