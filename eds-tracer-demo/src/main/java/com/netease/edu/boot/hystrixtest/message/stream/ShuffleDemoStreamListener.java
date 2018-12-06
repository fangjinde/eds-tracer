package com.netease.edu.boot.hystrixtest.message.stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.netease.edu.boot.hystrixtest.dto.DemoDto;
import com.netease.edu.boot.hystrixtest.message.stream.binding.ShuffleStreamBinding;

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
