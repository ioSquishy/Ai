package ai;

import java.util.concurrent.TimeUnit;

import ai.Utility.TaskScheduler;

public class AppTest {
    public static void main(String[] args) throws Exception {
        System.out.println("submitted task");
        TaskScheduler.scheduleTask(
                "test",
                () -> {System.out.println("runnable ran");},
                3, TimeUnit.SECONDS);
        
        System.out.println("gateway disconnected");
        App.gatewayDisconnected = true;

        System.out.println("sleeping....");
        Thread.sleep(5000);

        System.out.println("gateway reconnecting");
        App.gatewayDisconnected = false;
        TaskScheduler.gatewayReconnectListener();
    }
}
