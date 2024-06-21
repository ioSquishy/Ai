package ai;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bson.Document;

import ai.Constants.DatabaseKey;

public class AppTest {
    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        String json = "{\"joinRoleIDs\": [846451583550095371]}";
        Document parsedList = Document.parse(json);

        // Document document = new Document()
        //     .append("joinRoleIDs", parsedList.getList("joinRoleIDs", Object.class, Collections.EMPTY_LIST));

        List<Object> objList = parsedList.getList("joinRoleIDs", Object.class, Collections.EMPTY_LIST);
        List<Long> longList = objList.stream().mapToLong(obj -> {
            if (obj instanceof Number) {
                return ((Number)obj).longValue();
            } else {
                return -1;
            }
        }).distinct().boxed().toList();

        System.out.println(longList);
    }
}
