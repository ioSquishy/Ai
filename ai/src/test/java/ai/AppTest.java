package ai;

public class AppTest {
    public static void main(String[] args) {
        test(null);
    }

    public static void test(String reason) {
        System.out.println(reason);
        reason = reason != null ? reason : "";
        System.out.println(reason);
    }
}
