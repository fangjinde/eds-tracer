package com.netease.edu.boot.hystrixclient.message.stream.binding;

import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;

public interface ShuffleStreamBindingForClient {

    String SHUFFLE_STREAM_OUTPUT = "shuffleStreamOutput";

    @Output(SHUFFLE_STREAM_OUTPUT)
    MessageChannel shuffleStreamOutput();

}
