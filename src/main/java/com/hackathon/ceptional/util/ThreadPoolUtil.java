package com.hackathon.ceptional.util;

import com.hackathon.ceptional.config.Constants;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * for running async tasks
 *
 * @author Liping
 * @version 1.0.0
 * @date 2020/2/13
 */
public class ThreadPoolUtil {
    /**
     * 创建一个是十六路线程池
     */
    private static final ScheduledExecutorService SCHEDULER = new ScheduledThreadPoolExecutor(Constants.THREAD_COUNT,
            new BasicThreadFactory.Builder().namingPattern("schedule-pool-%02d").daemon(true).build());

    public static void executeMultiThread(Runnable runnable) {
        SCHEDULER.execute(runnable);
    }
}
