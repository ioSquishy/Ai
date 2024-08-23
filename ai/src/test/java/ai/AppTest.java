package ai;

import ai.Data.Database;
import ai.Data.ServerDocument;

public class AppTest {
    public static void main(String[] args) throws Exception {
        Database.initMongoDB();
        
        ServerDocument serverDoc = Database.getServerDoc(791040843279630356L);
        // System.out.println(serverDoc.test.aString);
        System.out.println(serverDoc.toString());
    }
}
