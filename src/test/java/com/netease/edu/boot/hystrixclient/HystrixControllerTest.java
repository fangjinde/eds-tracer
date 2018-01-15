package com.netease.edu.boot.hystrixclient;/**
 * Created by hzfjd on 18/1/15.
 */

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;

/**
 * @author hzfjd
 * @create 18/1/15
 */
public class HystrixControllerTest extends HystrixClientBaseTest {


    @Autowired
    RestTemplate restTemplate;

    @Test
    public void test_echoWithoutFallback_normal(){
        restTemplate.getForObject("/echoWithoutFallback?testCase={testCase}",String.class,0);
    }

    @Test
    public void test_echoWithoutFallback_ignorableException(){

    }
    @Test
    public void test_echoWithoutFallback_unIgnorableException(){


    }
}
