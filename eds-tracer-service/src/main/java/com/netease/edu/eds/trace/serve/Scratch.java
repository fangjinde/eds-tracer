package com.netease.edu.eds.trace.serve;

import org.joda.time.Duration;

/**
 * @author hzfjd
 * @create 18/12/18
 **/
public class Scratch {

    public static void main(String[] args) {
        Duration duration=  Duration.parse("PT0.1S");
        System.out.println(duration.getMillis());
    }

}
