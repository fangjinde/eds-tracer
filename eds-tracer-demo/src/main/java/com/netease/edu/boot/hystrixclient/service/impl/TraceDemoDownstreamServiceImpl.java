package com.netease.edu.boot.hystrixclient.service.impl;

import com.netease.edu.boot.hystrixclient.service.TraceDemoDownstreamService;
import org.springframework.stereotype.Component;

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
