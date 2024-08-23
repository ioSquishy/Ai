package ai.Data;

import io.github.cdimascio.dotenv.Dotenv;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.mongodb.client.model.Filters.eq;
import org.bson.Document;
import org.javacord.api.interaction.InteractionBase;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.ReplaceOptions;

import ai.App;
import ai.Constants.DatabaseKey;

public class Database implements Serializable {
    private static transient final String connectionString = "mongodb+srv://squishydb:" + Dotenv.load().get("MONGO_PASS") + "@sandbox.ujgwpn6.mongodb.net/?retryWrites=true&w=majority";
    private static transient MongoClient mongoClient;
    private static transient MongoDatabase mongoDatabase;
    private static transient MongoCollection<Document> mongoServerCollection;
    public static transient boolean mongoOK = true;
    public static transient Long downUpTimeStartEpoch = Instant.now().getEpochSecond(); // record both how long the database has been up/down

    private static HashMap<Long, Document> serverCache = new HashMap<Long, Document>();
    private static final transient int currentDocumentVersion = 4;

    private static transient ScheduledExecutorService autoCacheExe = Executors.newSingleThreadScheduledExecutor();
    private static transient Runnable autoCache = () -> {
        if (mongoOK) {
            syncCacheToDatabase();
        } else {
            mongoNotOK();
        }
    };
    private static transient Runnable checkMongo = () -> {
        initMongoDB();
    };

    private static void resetDownUpTime() {
        downUpTimeStartEpoch = Instant.now().getEpochSecond();
    }

    public static void initMongoDB() {
        try {
            mongoClient = MongoClients.create(connectionString);
            mongoDatabase = mongoClient.getDatabase("dev");
            mongoServerCollection = mongoDatabase.getCollection("ai-servers");
            // getDatabase will throw an exception if cluster is unreachable
            System.out.println("Database Connected");

            if (mongoOK) { // if mongo was ok previously
                autoCacheExe.scheduleAtFixedRate(autoCache, 10, 10, TimeUnit.SECONDS);
            } else {
                mongoOK = true;
                resetDownUpTime();
                autoCacheExe.shutdownNow();
                autoCacheExe = Executors.newSingleThreadScheduledExecutor();
                autoCacheExe.scheduleAtFixedRate(autoCache, 10, 10, TimeUnit.SECONDS);
            }
            System.out.println("autoCacheExe running!");
        } catch (Exception e) {
            e.printStackTrace();
            mongoNotOK();
        }
    }

    private static void mongoNotOK() {
        System.out.println("Mongo is NOT OK!!");
        // App.api.getUserById("263049275196309506").join().sendMessage("MongoDB failed to connect :(");
        if (mongoOK) {
            mongoOK = false;
            resetDownUpTime();
            saveCacheLocally();
            autoCacheExe.shutdownNow();
            autoCacheExe = Executors.newSingleThreadScheduledExecutor();
            autoCacheExe.scheduleAtFixedRate(checkMongo, 10, 10, TimeUnit.MINUTES);
        }
    }

    private static void saveCacheLocally() {
        try {
            File file = new File("cache.ser");
            file.createNewFile();
            FileOutputStream fos = new FileOutputStream(file);
            ObjectOutputStream out = new ObjectOutputStream(fos);
            out.writeObject(serverCache);
            out.close();
            fos.close();
        } catch (Exception e) {
            App.api.getUserById("263049275196309506").join().sendMessage("Cache did not manually save... RIP");
            App.api.disconnect();
        }
    }

    public static Document cloneDocument(Document toClone) {
        return new Document(toClone);
    }

    public static Document getServerDoc(long serverID) throws DocumentUnavailableException {
        System.out.println(serverID);
        Document doc = checkCache(serverID);
        if (doc != null) { // if doc in cache return cached doc
            return doc;
        } else { // if doc not in cache check database
            doc = checkDatabase(serverID);
        }
        if (doc != null) { // if doc in database add doc to cache and return doc
            putDocInCache(serverID, doc);
            return doc;
        } else if (mongoOK) { // if doc not retrieved from database check if mongo is ok
            // if mongo is ok create a new doc in database and add to cache
            doc = createNewDoc(serverID);
            putDocInCache(serverID, doc);
            return doc;
        } else {
            throw new DocumentUnavailableException("Database unreachable and server not in cache.");
        }
    }
    private static Document checkCache(long userID) {
        return serverCache.get(userID);
    }
    private static Document checkDatabase(long userID) {
        Document document = mongoOK ? mongoServerCollection.find(eq(DatabaseKey.id, userID)).first() : null;
        if (document != null && document.getInteger("documentVersion", -1) != currentDocumentVersion) {
            document = updateDocument(document.getLong(DatabaseKey.id), document, new Document());
        }
        return document;
    }
    private static Document createNewDoc(long serverID) {
        return updateDocument(serverID, null, null);
    }
    public static void putDocInCache(long serverID, Document document) {
        serverCache.put(serverID, document);
    }

    
    @SuppressWarnings("unchecked")
    public static Document updateDocument(long serverID, Document original, Document updates) throws ClassCastException {
        original = original != null ? original : new Document();
        updates = updates != null ? updates : new Document();
        return new Document()
            .append(DatabaseKey.id, (Long) serverID)
            .append(DatabaseKey.documentVersion, (int) currentDocumentVersion)
            .append(DatabaseKey.lastCommand, (Long) Instant.now().getEpochSecond()/60)
            .append(DatabaseKey.muteRoleID, (Long) updates.getOrDefault(DatabaseKey.muteRoleID, original.getOrDefault(DatabaseKey.muteRoleID, null)))
            .append(DatabaseKey.modLogEnabled, (boolean) updates.getBoolean(DatabaseKey.modLogEnabled, original.getBoolean(DatabaseKey.modLogEnabled, false))) // mod log stuff
            .append(DatabaseKey.aiModEnabled, (boolean) updates.getBoolean(DatabaseKey.aiModEnabled, original.getBoolean(DatabaseKey.aiModEnabled, false)))
            .append(DatabaseKey.logChannelID, (Long) updates.getOrDefault(DatabaseKey.logChannelID, original.getOrDefault(DatabaseKey.logChannelID, null)))
            .append(DatabaseKey.logBans, (boolean) updates.getBoolean(DatabaseKey.logBans, original.getBoolean(DatabaseKey.logBans, false)))
            .append(DatabaseKey.logMutes, (boolean) updates.getBoolean(DatabaseKey.logMutes, original.getBoolean(DatabaseKey.logMutes, false)))
            .append(DatabaseKey.logKicks, (boolean) updates.getBoolean(DatabaseKey.logKicks, original.getBoolean(DatabaseKey.logKicks, false)))
            .append(DatabaseKey.joinMessageEnabled, (boolean) updates.getBoolean(DatabaseKey.joinMessageEnabled, original.getBoolean(DatabaseKey.joinMessageEnabled, false))) // cosmetics
            .append(DatabaseKey.joinMessageChannelID, (Long) updates.getOrDefault(DatabaseKey.joinMessageChannelID, original.getOrDefault(DatabaseKey.joinMessageChannelID, null)))
            .append(DatabaseKey.joinMessage, (String) updates.getOrDefault(DatabaseKey.joinMessage, original.getOrDefault(DatabaseKey.joinMessage, null)))
            .append(DatabaseKey.joinRoleIDs, 
                (List<Long>) getListOrDefault(
                    getLongList(updates, DatabaseKey.joinRoleIDs), 
                    getListOrDefault(
                        getLongList(original, DatabaseKey.joinRoleIDs), Collections.EMPTY_LIST)));
    }

    public static List<Long> getLongList(Document document, String key) {
        return getLongListFromObjectList(document.getList(key, Object.class));
    }

    private static List<?> getListOrDefault(List<?> list, List<?> orDefault) {
        return list != null ? list : orDefault;
    }
    private static List<Long> getLongListFromObjectList(List<Object> objectList) throws ClassCastException {
        if (objectList == null) return null;
        List<Long> longList = objectList.stream().mapToLong(obj -> {
            if (obj instanceof Number) {
                return ((Number)obj).longValue();
            } else {
                return -1;
            }
        }).boxed().toList();
        
        if (longList.stream().noneMatch(num -> num == -1)) {
            return longList;
        } else {
            throw new ClassCastException("Could not cast List<Object> to List<Long>");
        }
    }

    private static transient final ReplaceOptions replaceOpts = new ReplaceOptions().upsert(true);
    private static transient final BulkWriteOptions bulkOpts = new BulkWriteOptions().ordered(false);
    private static void syncCacheToDatabase() {
        HashMap<Long, Document> cacheCopy = new HashMap<Long, Document>(serverCache);
        ArrayList<ReplaceOneModel<Document>> writeReqs = new ArrayList<ReplaceOneModel<Document>>(cacheCopy.size());
        long currentMinute = Instant.now().getEpochSecond()/60;
        for (Map.Entry<Long, Document> entry : cacheCopy.entrySet()) {
            writeReqs.add(new ReplaceOneModel<Document>(eq(DatabaseKey.id, entry.getKey()), entry.getValue(), replaceOpts));
            if (currentMinute - entry.getValue().get(DatabaseKey.lastCommand, Instant.now().getEpochSecond()/60) > 30) { //removes doc from cache if they havent used a cmd in 30 mins
                serverCache.remove(entry.getKey());
            }
        }
        try {
            if (!writeReqs.isEmpty()) {
                mongoServerCollection.bulkWrite(writeReqs, bulkOpts);
            }
        } catch (Exception e) {
            e.printStackTrace();
            mongoNotOK();
        }
    }

    public static void removeServer(long serverID) {
        serverCache.remove(serverID);
        mongoServerCollection.deleteOne(eq(DatabaseKey.id, serverID));
    }

    public static void clearCache() {
        serverCache.clear();
    }

    public static class DocumentUnavailableException extends Exception {
        public DocumentUnavailableException() {
            super();
        }

        public DocumentUnavailableException(String message) {
            super(message);
        }

        public static String getStandardResponseString() {
            return "Database is currently unreachable. Please try again later.";
        }

        public static void sendStandardResponse(InteractionBase interaction) {
            interaction.createImmediateResponder().setContent(getStandardResponseString()).respond();
        }
    }
}