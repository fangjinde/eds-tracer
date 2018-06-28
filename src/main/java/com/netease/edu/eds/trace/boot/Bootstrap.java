package com.netease.edu.eds.trace.boot;

import java.lang.instrument.Instrumentation;
import java.util.Map;

/**
 * @author hzfjd
 * @create 18/6/28
 **/
public class Bootstrap {

    public static void start(Instrumentation instumentation, Map<String, String> props) {
        PluginLoader.load();
    }

}
