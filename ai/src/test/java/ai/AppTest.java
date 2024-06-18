package ai;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class AppTest {
    static int msgCount = 0;
    static long fiveMsgsAgoEpoch = 0;
    static ArrayList<String> backlog = new ArrayList<String>();
    static ScheduledExecutorService backlogExecutor = Executors.newSingleThreadScheduledExecutor();
    static ScheduledFuture<?> blExeTask;
    static Runnable flushOne = () -> {
        if (!backlog.isEmpty()) {
            System.out.println(backlog.remove(0) + " out");
        } else {
            blExeTask.cancel(false);
        }
    };
    public static void main(String[] args) {
        Scanner scan = new Scanner(System.in);
        
        while (true) {
            String join = scan.nextLine();
            System.out.println(backlog);
            if (!backlog.isEmpty()) {
                backlog.add(join);
                continue;
            }
            if (msgCount >= 5) {
                //fiveAgo is within 4 seconds
                if (Instant.now().toEpochMilli() - fiveMsgsAgoEpoch <= 4000) {
                    backlog.add(join);
                    startBacklogFlush();
                    msgCount = 0;
                    fiveMsgsAgoEpoch = 0;
                    continue;
                } else {
                    msgCount = 0;
                    fiveMsgsAgoEpoch = Instant.now().toEpochMilli();
                    System.out.println(join + " out");
                }
            } else {
                msgCount++;
                System.out.println(join + " out");
            }
        }
    }

    public static void startBacklogFlush() {
        blExeTask = backlogExecutor.scheduleWithFixedDelay(flushOne, 2, 2, TimeUnit.SECONDS);
    }


}
