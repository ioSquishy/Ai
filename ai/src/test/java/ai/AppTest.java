package ai;

import java.util.concurrent.TimeUnit;

import ai.Utility.TaskScheduler;

public class AppTest {
    public static void main(String[] args) throws Exception {
        // submit task to go off in 5 seconds
        TaskScheduler.scheduleTask(
                "test",
                () -> {System.out.println("runnable ran");},
                3, TimeUnit.SECONDS);
        
        App.gatewayDisconnected = true;

        Thread.sleep(5000);

        App.gatewayDisconnected = false;
    }
}
