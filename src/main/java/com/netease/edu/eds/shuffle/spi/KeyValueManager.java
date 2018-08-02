package com.netease.edu.eds.shuffle.spi;

public interface KeyValueManager {

    String getValue(String key);

    void setValue(String key, String value);

    Long increment(String key, long delta);
}
