package com.bean.common.utils;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadPoolManager {

    private static ThreadPoolManager instance = new ThreadPoolManager();

    public static ThreadPoolManager getInstance() {
        return instance;
    }

    //线程池
    private ThreadPoolExecutor threadPoolExecutor;
    //请求队列
    private LinkedBlockingQueue<Future<?>> service = new LinkedBlockingQueue<>();

    //初始化
    private ThreadPoolManager() {

        threadPoolExecutor = new ThreadPoolExecutor(4, 10, 10, TimeUnit.SECONDS,
                new ArrayBlockingQueue<Runnable>(4));

        threadPoolExecutor.execute(runnable);
    }

    //请求队列里的线程任务排队到线程池执行
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {

            while (true) {
                FutureTask futureTask = null;

                try {
                    futureTask = (FutureTask) service.take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (futureTask != null) {
                    threadPoolExecutor.execute(futureTask);
                }
            }
        }
    };

    //执行的线程任务,延时多少秒执行
    public <T> void execute(final FutureTask<T> futureTask, Object delayed) {


        if (futureTask != null) {
            try {
                //延时执行
                if (delayed != null) {
                    Timer timer = new Timer();
                    timer.schedule(new TimerTask() {
                        public void run() {
                            try {
                                service.put(futureTask);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                        }
                    }, (long) delayed);
                } else {
                    service.put(futureTask);

                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

    }
}


