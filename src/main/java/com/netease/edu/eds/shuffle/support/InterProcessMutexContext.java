package com.netease.edu.eds.shuffle.support;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.utils.ZKPaths;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 不依赖base-framework。单独实现。
 * 
 * @author hzfjd
 * @create 18/8/2
 **/
public class InterProcessMutexContext {

    public static final String                              DEFAULT_LOCK_ROOT   = "/ShuffleLockRoot";
    private static final int                                DEFAULT_CONCURRENCY = 1024;
    private String                                          lockRoot            = DEFAULT_LOCK_ROOT;
    private String                                          bizStage;
    private int                                             concurrency         = DEFAULT_CONCURRENCY;
    private CuratorFramework                                client;
    private String                                          lockPath            = null;
    private HashCalculator                                  hashCalculator      = null;

    private static ConcurrentMap<String, InterProcessMutex> lockConcurrentMap   = new ConcurrentHashMap<>();

    public InterProcessMutexContext(CuratorFramework client, String bizStage) {
        this(client, bizStage, -1);
    }

    public InterProcessMutexContext(CuratorFramework client, String bizStage, int concurrency) {
        this(client, null, bizStage, concurrency);
    }

    public InterProcessMutexContext(CuratorFramework client, String lockRoot, String bizStage, int concurrency) {
        this.client = client;
        if (lockRoot != null) {
            this.lockRoot = lockRoot;
        }
        this.bizStage = bizStage;
        if (concurrency > 0) {
            this.concurrency = concurrency;
        }
        hashCalculator = new HashCalculator(concurrency);
        lockPath = ZKPaths.makePath(lockRoot, bizStage);

    }

    public InterProcessMutex getLock(String lockKey) {

        if (lockKey == null) {
            throw new IllegalArgumentException("lockKey is null");
        }

        int segmentIndex = hashCalculator.getIndexOfValue(lockKey.hashCode());
        String path2Lock = ZKPaths.makePath(lockPath, String.valueOf(segmentIndex));

        InterProcessMutex mutex = lockConcurrentMap.get(path2Lock);
        if (mutex != null) {
            return mutex;
        }

        synchronized (this) {
            mutex = lockConcurrentMap.get(path2Lock);
            if (mutex != null) {
                return mutex;
            }
            mutex = new InterProcessMutex(client, path2Lock);
            InterProcessMutex previous = lockConcurrentMap.putIfAbsent(path2Lock, mutex);
            return previous == null ? mutex : previous;

        }

    }

    class HashCalculator {

        static final int MAX_SEGMENTS = 65536;
        final int        segmentMask;
        final int        segmentShift;

        public HashCalculator(int concurrencyLevel) {
            if (concurrencyLevel > 65536) {
                concurrencyLevel = 65536;
            }

            int sshift = 0;

            int ssize;
            for (ssize = 1; ssize < concurrencyLevel; ssize <<= 1) {
                ++sshift;
            }

            this.segmentShift = 32 - sshift;
            this.segmentMask = ssize - 1;
        }

        private int jenkinsHash(int h) {
            h += h << 15 ^ -12931;
            h ^= h >>> 10;
            h += h << 3;
            h ^= h >>> 6;
            h += (h << 2) + (h << 14);
            return h ^ h >>> 16;
        }

        public int getIndexOfValue(int value) {
            int hash = this.jenkinsHash(value);
            return hash >>> this.segmentShift & this.segmentMask;
        }

    }

}
