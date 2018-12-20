package com.netease.edu.eds.trace.clientdemo.web.controller;

import brave.propagation.ExtraFieldPropagation;
import com.netease.edu.eds.trace.clientdemo.message.stream.binding.ShuffleStreamBindingForClient;
import com.netease.edu.eds.trace.democommon.dto.DemoDto;
import com.netease.edu.eds.trace.democommon.service.TraceDemoService;
import com.netease.edu.web.viewer.ResponseView;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author hzfjd
 * @create 18/7/18
 **/
@RestController
public class ShuffleClientDemoController {

    @Autowired TraceDemoService traceDemoService;

    @Autowired
    @Qualifier("shuffleDemoAmqpTemplate")
    AmqpTemplate           shuffleDemoAmqpTemplate;

    @Autowired
    @Qualifier("shuffleRetryDemoAmqpTemplate")
    AmqpTemplate           shuffleRetryDemoAmqpTemplate;

    @Resource(name = ShuffleStreamBindingForClient.SHUFFLE_STREAM_OUTPUT)
    private MessageChannel messageChannel;

    @RequestMapping(path = "client/redirect")
    public void redirect(@RequestParam(value = "redirectUrl") String redirectUrl,
                         HttpServletResponse response) throws IOException {

        if (StringUtils.isNotBlank(redirectUrl)) {
            response.sendRedirect(redirectUrl);
            return;
        }

        response.sendRedirect("/client/redirectLandPage");
        return;
    }

    @RequestMapping(path = "client/redirectLandPage")
    public ResponseView redirectLandPage() {
        ResponseView responseView = new ResponseView();
        Map<String, Object> map = new HashMap<>();
        String echo = traceDemoService.doSth("redirect land page");
        map.put("echo", echo);
        responseView.setResult(map);
        return responseView;
    }

    @RequestMapping(path = "client/dubboOps")
    public ResponseView dubboOps() {
        ResponseView responseView = new ResponseView();
        Map<String, Object> map = new HashMap<>();
        String echo = traceDemoService.doSth("from TraceClientDemoController");
        map.put("echo", echo);
        responseView.setResult(map);
        return responseView;
    }

    @RequestMapping(path = "client/dubboError")
    public ResponseView dubboError() {
        ResponseView responseView = new ResponseView();
        Map<String, Object> map = new HashMap<>();
        traceDemoService.fail();
        map.put("call", "OK");
        responseView.setResult(map);
        return responseView;
    }

    @RequestMapping(path = "client/rabbitOps")
    public ResponseView rabbitOps() {
        ResponseView responseView = new ResponseView();

        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("messageKey", "hzfjd");

        shuffleDemoAmqpTemplate.convertAndSend(messageMap);

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("send", "ok");
        responseView.setResult(resultMap);
        return responseView;
    }

    @RequestMapping(path = "client/rabbitRetryOps")
    public ResponseView rabbitRetryOps() {
        ResponseView responseView = new ResponseView();

        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("messageKey", "hzfjd");

        shuffleRetryDemoAmqpTemplate.convertAndSend(messageMap);

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("send", "ok");
        responseView.setResult(resultMap);
        return responseView;
    }

    @RequestMapping(path = "client/cloudStreamRabbitOps")
    public ResponseView cloudStreamRabbitOps() {
        ResponseView responseView = new ResponseView();

        ExtraFieldPropagation.set("provider-id", "hzfjd123");

        boolean sent = messageChannel.send(MessageBuilder.withPayload(new DemoDto().withName("hzfjd-cloud-stream-rabbit")).build());
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("send", sent);
        responseView.setResult(resultMap);
        return responseView;
    }

}
