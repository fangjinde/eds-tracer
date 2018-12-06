package com.netease.edu.boot.hystrixtest.service;

public interface TraceDemoService {

    String doSth(String args);

    String fail();
}
