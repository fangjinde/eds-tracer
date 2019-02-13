package com.netease.edu.eds.trace.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;

/**
 * @author hzfjd
 * @create 18/5/25
 **/
public class TraceJsonUtils {

    private static final Logger logger = LoggerFactory.getLogger(TraceJsonUtils.class);

    public static String toJson(Object object) {
        try {
            // must not use jackson ObjectMapper, which has mutual cycle reference bug.
            return JSON.toJSONString(object);

        } catch (Exception e) {
            logger.error("toJson error", e);
            return ExceptionStringUtils.getStackTraceString(e);
        }

    }



    public static class Person {

        public Person(String name) {
            this.name = name;
        }

        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        private Person parent;

        private Person child;

        public Person getChild() {
            return child;
        }

        public void setChild(Person child) {
            this.child = child;
        }

        public Person getParent() {
            return parent;
        }

        public void setParent(Person parent) {
            this.parent = parent;
        }
    }

    public static void main(String[] args) {

        Person jack = new Person("jack");
        Person jackson = new Person("jackson");
        // self cycle
        // jackson.setParent(jackson);

        // mutual cycle
        jackson.setParent(jack);
        jack.setChild(jackson);

        System.out.println(TraceJsonUtils.toJson(jackson));

    }
}
