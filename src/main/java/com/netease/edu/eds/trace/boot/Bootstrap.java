package com.netease.edu.eds.trace.boot;

import java.lang.instrument.Instrumentation;
import java.util.Map;

/**
 * we must load instruments after spring boot class loader is set.
 * 
 * @author hzfjd
 * @create 18/6/28
 **/
@Deprecated
public class Bootstrap {

    public static void start(Instrumentation instumentation, Map<String, String> props) {
        PluginLoader.load();
    }

}
