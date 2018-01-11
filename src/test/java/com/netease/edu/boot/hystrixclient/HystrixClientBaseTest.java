package com.netease.edu.boot.hystrixclient;/**
 * Created by hzfjd on 18/1/11.
 */

import org.junit.runner.RunWith;
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

}
