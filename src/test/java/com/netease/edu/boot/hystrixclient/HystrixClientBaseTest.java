package com.netease.edu.boot.hystrixclient;/**
 * Created by hzfjd on 18/1/11.
 */

import com.netease.edu.boot.hystrixtest.service.HystrixTestNoFallbackService;
import com.netease.edu.boot.hystrixtest.service.HystrixTestWithFallbackService;
import com.netease.edu.util.exceptions.FrontNotifiableRuntimeException;
import com.netease.edu.util.exceptions.SystemErrorRuntimeException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author hzfjd
 * @create 18/1/11
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { HystrixTestClient.class }, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties={"hystrix.command.C.default.execution.isolation.thread.timeoutInMilliseconds=600000","spring.profiles.active=mac-linux-dev,client"})
public class HystrixClientBaseTest {

    @Autowired
    HystrixTestWithFallbackService hystrixTestWithFallbackService;

    @Autowired
    HystrixTestNoFallbackService hystrixTestNoFallbackService;

    @Test
    public void test_echo_normal() {
        Assert.assertEquals("0", hystrixTestWithFallbackService.echoWithFallbackSupport(0));
        Assert.assertEquals("0", hystrixTestWithFallbackService.echoWithoutFallbackSupport(0));
    }

    @Test(expected = FrontNotifiableRuntimeException.class)
    public void test_echo_ignoreExceptionWithFallback() {
        hystrixTestWithFallbackService.echoWithFallbackSupport(1);
    }

    @Test(expected = FrontNotifiableRuntimeException.class)
    public void test_echo_ignoreExceptionNoFallbackInstance() {
        hystrixTestNoFallbackService.echo(1);
    }

    @Test(expected = FrontNotifiableRuntimeException.class)
    public void test_echo_ignoreExceptionNoFallbackMethodSupported() {
        hystrixTestWithFallbackService.echoWithoutFallbackSupport(1);
    }

    @Test
    public void test_echo_unIgnorableExceptionWithFallback() {
        Assert.assertEquals(HystrixTestWithFallbackService.FALLBACK_PREFIX+2,  hystrixTestWithFallbackService.echoWithFallbackSupport(2));

    }



    @Test(expected = SystemErrorRuntimeException.class)
    public void test_echo_unIgnorableExceptionNoFallbackInstance() {
        hystrixTestNoFallbackService.echo(2);
    }

    @Test(expected = SystemErrorRuntimeException.class)
    public void test_echo_unIgnorableExceptionNoFallbackMethodSupported() {
        hystrixTestWithFallbackService.echoWithoutFallbackSupport(2);
    }

    @Test(expected = SystemErrorRuntimeException.class)
    public void test_echo_unIgnorableExceptionWithFallbackFailOnIgnoreException() {
        hystrixTestWithFallbackService.echoWithFallbackSupport(12);

    }

    @Test(expected = RuntimeException.class)
    public void test_echo_unIgnorableExceptionWithFallbackFailOnUnIgnorableException() {
        hystrixTestWithFallbackService.echoWithFallbackSupport(13);

    }

    @Test
    public void test_echo_unIgnorableDubboNotSupportedExceptionWithFallback() {
        Assert.assertEquals(HystrixTestWithFallbackService.FALLBACK_PREFIX+3,  hystrixTestWithFallbackService.echoWithFallbackSupport(3));

    }

    @Test(expected = RuntimeException.class)
    public void test_echo_unIgnorableDubboNotSupportedExceptionNoFallbackInstance() {
        hystrixTestNoFallbackService.echo(3);
    }

    @Test(expected = RuntimeException.class)
    public void test_echo_unIgnorableDubboNotSupportedExceptionNoFallbackMethodSupported() {
        hystrixTestWithFallbackService.echoWithoutFallbackSupport(3);
    }

}
