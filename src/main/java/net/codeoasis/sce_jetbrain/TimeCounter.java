package net.codeoasis.sce_jetbrain;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class TimeCounter {

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> scheduledTask;
    private long secondsElapsed = 0;
    private static TimeCounter instance = null;
    public static TimeCounter getInstance() {
        if (instance == null) {
            instance = new TimeCounter();
        }
        return instance;
    }

    // Start the counter
    public void startCounter() {
        if (scheduledTask == null || scheduledTask.isCancelled()) {
            scheduledTask = scheduler.scheduleAtFixedRate(() -> {
                if (OasisActivator.isAppActive()) {
                    secondsElapsed++;
                    System.out.println("Time elapsed: " + secondsElapsed + " seconds");
                }
            }, 0, 1, TimeUnit.SECONDS);
        }
    }

    // Stop the counter
    public void stopCounter() {
        if (scheduledTask != null && !scheduledTask.isCancelled()) {
            scheduledTask.cancel(true);
        }
    }

    // Reset the counter
    public void resetCounter() {
        stopCounter();
        secondsElapsed = 0;
        System.out.println("Counter reset.");
    }

    public long getSecondsElapsed() {
        return secondsElapsed;
    }
//
//    public static void main(String[] args) {
//        TimeCounter counter = new TimeCounter();
//
//        // Start counting
//        counter.startCounter();
//
//        // Example usage: stop the counter after 10 seconds
//        try {
//            Thread.sleep(10000); // Simulate some delay
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//
//        // Stop the counter
//        counter.stopCounter();
//        System.out.println("Final time elapsed: " + counter.getSecondsElapsed() + " seconds");
//    }
}
