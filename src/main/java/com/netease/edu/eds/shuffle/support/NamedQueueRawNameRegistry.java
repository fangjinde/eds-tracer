package com.netease.edu.eds.shuffle.support;

import com.netease.edu.eds.trace.utils.ConcurrentHashSet;

import java.util.Iterator;
import java.util.Set;

/**
 * @author hzfjd
 * @create 18/8/23
 **/
public class NamedQueueRawNameRegistry {

    private static Set<String> allNamedQueueRawNames = new ConcurrentHashSet<>();

    public static void add(String name) {
        allNamedQueueRawNames.add(name);
    }

    public static boolean contain(String name) {
        return allNamedQueueRawNames.contains(name);
    }

    public static Iterator<String> getIterator() {
        return allNamedQueueRawNames.iterator();
    }
}
