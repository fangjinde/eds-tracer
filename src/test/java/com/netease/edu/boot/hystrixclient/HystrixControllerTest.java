package com.netease.edu.boot.hystrixclient;/**
 * Created by hzfjd on 18/1/15.
 */

import com.netease.edu.web.viewer.ResponseView;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * @author hzfjd
 * @create 18/1/15
 */
public class HystrixControllerTest extends HystrixClientBaseTest {

    RestTemplateBuilder restTemplateBuilder;

    @Autowired
    public void setRestTemplateBuilder(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplateBuilder = restTemplateBuilder;
        restTemplate = restTemplateBuilder.rootUri("http://127.0.0.1:20022/web").build();
    }

    RestTemplate restTemplate;

    @Test
    public void test_echoWithoutFallback_normal() {
        ResponseView responseView = restTemplate.getForObject("/echoWithoutFallback?testCase={testCase}",
                                                              ResponseView.class, 0);
        Assert.assertTrue(responseView.getResult().equals(new Integer(0)));
    }

    @Test(expected = RestClientException.class)
    public void test_echoWithoutFallback_ignorableException() {
        ResponseView responseView = restTemplate.getForObject("/echoWithoutFallback?testCase={testCase}",
                                                              ResponseView.class, 11);
    }

    @Test(expected = RestClientException.class)
    public void test_echoWithoutFallback_unIgnorableException() {
        ResponseView responseView = restTemplate.getForObject("/echoWithoutFallback?testCase={testCase}",
                                                              ResponseView.class, 12);
    }

    @Test
    public void test_echoWithoutFallback_ignorableImplicitExceptionInResultMode() {
        ResponseView responseView = restTemplate.getForObject("/echoWithoutFallback?testCase={testCase}",
                                                              ResponseView.class, 1);
        Assert.assertTrue(responseView.getCode() == ResponseView.OK_NOT_LOGIN);
    }

    @Test
    public void test_echoWithoutFallback_unIgnorableImplicitExceptionInResultMode() {
        ResponseView responseView = restTemplate.getForObject("/echoWithoutFallback?testCase={testCase}",
                                                              ResponseView.class, 2);
        Assert.assertTrue(responseView.getCode() == ResponseView.EXCEPTION_IGNORE);
    }

    @Test
    public void test_echo_normal() {
        ResponseView responseView = restTemplate.getForObject("/echo?testCase={testCase}&fallbackDepth=0",
                                                              ResponseView.class, 0);
        Assert.assertTrue(responseView.getResult().equals(new Integer(0)));
    }

    @Test(expected = RestClientException.class)
    public void test_echo_ignorableExceptionNoFallback() {
        ResponseView responseView = restTemplate.getForObject("/echo?testCase={testCase}&fallbackDepth=0",
                                                              ResponseView.class, 11);
    }

    @Test(expected = RestClientException.class)
    public void test_echo_unIgnorableExceptionNoFallback() {
        ResponseView responseView = restTemplate.getForObject("/echo?testCase={testCase}&fallbackDepth=0",
                                                              ResponseView.class, 12);
    }

    @Test
    public void test_echo_ignorableImplicitExceptionInResultModeNoFallback() {
        ResponseView responseView = restTemplate.getForObject("/echo?testCase={testCase}&fallbackDepth=0",
                                                              ResponseView.class, 1);
        Assert.assertTrue(responseView.getCode() == ResponseView.OK_NOT_LOGIN);
    }

    @Test
    public void test_echo_unIgnorableImplicitExceptionInResultModeNoFallback() {
        ResponseView responseView = restTemplate.getForObject("/echo?testCase={testCase}&fallbackDepth=0",
                                                              ResponseView.class, 2);
        Assert.assertTrue(responseView.getCode() == ResponseView.EXCEPTION_IGNORE);
    }

    @Test(expected = RestClientException.class)
    public void test_echo_ignorableExceptionWith1Fallback() {
        ResponseView responseView = restTemplate.getForObject("/echo?testCase={testCase}&fallbackDepth=1",
                                                              ResponseView.class, 11);

    }

    @Test
    public void test_echo_unIgnorableExceptionWith1Fallback() {
        ResponseView responseView = restTemplate.getForObject("/echo?testCase={testCase}&fallbackDepth=1",
                                                              ResponseView.class, 12);
        Assert.assertTrue("fallback1".equals(responseView.getResult()));
    }

    @Test
    public void test_echo_ignorableImplicitExceptionInResultModeWith1Fallback() {
        ResponseView responseView = restTemplate.getForObject("/echo?testCase={testCase}&fallbackDepth=1",
                                                              ResponseView.class, 1);
        Assert.assertTrue(responseView.getCode() == ResponseView.OK_NOT_LOGIN);
    }

    @Test
    public void test_echo_unIgnorableImplicitExceptionInResultModeWith1Fallback() {
        ResponseView responseView = restTemplate.getForObject("/echo?testCase={testCase}&fallbackDepth=1",
                                                              ResponseView.class, 2);
        Assert.assertTrue("fallback1".equals(responseView.getResult()));
    }

    @Test(expected = RestClientException.class)
    public void test_echo_ignorableExceptionWith2Fallback() {
        ResponseView responseView = restTemplate.getForObject("/echo?testCase={testCase}&fallbackDepth=2",
                                                              ResponseView.class, 11);

    }

    @Test
    public void test_echo_unIgnorableExceptionWith2Fallback() {
        ResponseView responseView = restTemplate.getForObject("/echo?testCase={testCase}&fallbackDepth=2",
                                                              ResponseView.class, 12);
        Assert.assertTrue("fallback2".equals(responseView.getResult()));
    }

    @Test
    public void test_echo_ignorableImplicitExceptionInResultModeWith2Fallback() {
        ResponseView responseView = restTemplate.getForObject("/echo?testCase={testCase}&fallbackDepth=2",
                                                              ResponseView.class, 1);
        Assert.assertTrue(responseView.getCode() == ResponseView.OK_NOT_LOGIN);
    }

    @Test
    public void test_echo_unIgnorableImplicitExceptionInResultModeWith2Fallback() {
        ResponseView responseView = restTemplate.getForObject("/echo?testCase={testCase}&fallbackDepth=2",
                                                              ResponseView.class, 2);
        Assert.assertTrue("fallback2".equals(responseView.getResult()));
    }

}
