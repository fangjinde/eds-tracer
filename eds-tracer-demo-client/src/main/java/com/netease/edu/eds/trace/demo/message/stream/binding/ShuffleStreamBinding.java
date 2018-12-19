package com.netease.edu.eds.trace.demo.message.stream.binding;

import org.springframework.cloud.stream.annotation.Input;
import org.springframework.messaging.SubscribableChannel;

public interface ShuffleStreamBinding {

    String SHUFFLE_STREAM_INPUT = "shuffleStreamInput";

    @Input(SHUFFLE_STREAM_INPUT)
    SubscribableChannel shuffleStreamInput();

}
