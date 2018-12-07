package com.netease.edu.eds.trace.demo.message.stream;

import com.alibaba.fastjson.JSON;
import com.netease.edu.eds.trace.demo.dto.DemoDto;
import com.netease.edu.eds.trace.demo.message.stream.binding.ShuffleStreamBinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.stereotype.Component;

/**
 * @author hzfjd
 * @create 18/8/20
 **/
@Component
public class ShuffleDemoStreamListener {

    private static final Logger logger = LoggerFactory.getLogger(ShuffleDemoStreamListener.class);

    @StreamListener(value = ShuffleStreamBinding.SHUFFLE_STREAM_INPUT)
    public void onShuffleMessage(DemoDto demoDto) {

        System.out.println(JSON.toJSONString(demoDto));
        logger.info(JSON.toJSONString(demoDto));

    }
}
