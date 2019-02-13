package com.netease.edu.eds.trace.utils;/**
 * Created by hzfjd on 18/3/27.
 */

import java.io.PrintWriter;

/**
 * @author hzfjd
 * @create 18/3/27
 */
public class ExceptionStringUtils {

    public static String getStackTraceString(Throwable e) {
        UnsafeStringWriter w = new UnsafeStringWriter();
        PrintWriter p = new PrintWriter(w);
        p.print(e.getClass().getName());
        if (e.getMessage() != null) {
            p.print(": " + e.getMessage());
        }
        p.println();
        try {
            e.printStackTrace(p);
            return w.toString();
        } finally {
            p.close();
        }
    }
}
