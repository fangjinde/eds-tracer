package com.netease.edu.boot.hystrixclient.service;

public interface TraceDemoDownstreamService {
    String doSth(String args);
    String fail();
}
