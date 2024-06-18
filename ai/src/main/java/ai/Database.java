package ai;

import io.github.cdimascio.dotenv.Dotenv;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.mongodb.client.model.Filters.eq;
import org.bson.Document;
import org.javacord.api.interaction.SlashCommandInteraction;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.ReplaceOptions;

public class Database implements Serializable {
    private static transient final String connectionString = "mongodb+srv://squishydb:" + Dotenv.load().get("MONGO_PASS") + "@sandbox.ujgwpn6.mongodb.net/?retryWrites=true&w=majority";
    private static transient MongoClient mongoClient;
    private static transient MongoDatabase mongoDatabase;
    private static transient MongoCollection<Document> mongoServerCollection;
    public static transient boolean mongoOK = true;

    private static HashMap<Long, Document> serverCache = new HashMap<Long, Document>();
    private static final transient int currentDocumentVersion = 3;

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
                autoCacheExe.shutdownNow();
                autoCacheExe = Executors.newSingleThreadScheduledExecutor();
                autoCacheExe.scheduleAtFixedRate(autoCache, 10, 10, TimeUnit.SECONDS);
            }
            System.out.println("autoCacheExe running!");
        } catch (Error e) {
            e.printStackTrace();
            mongoNotOK();
        }
    }

    private static void mongoNotOK() {
        System.out.println("Mongo is NOT OK!!");
        // App.api.getUserById("263049275196309506").join().sendMessage("MongoDB failed to connect :(");
        if (mongoOK) {
            mongoOK = false;
            saveCacheManually();
            autoCacheExe.shutdownNow();
            autoCacheExe = Executors.newSingleThreadScheduledExecutor();
            autoCacheExe.scheduleAtFixedRate(checkMongo, 10, 10, TimeUnit.MINUTES);
        }
    }

    private static void saveCacheManually() {
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

    public static Document getServerDoc(long serverID) throws DocumentUnavailableException {
        System.out.println(serverID);
        Document doc = checkCache(serverID);
        if (doc != null) { // if doc in cache return cached doc
            System.out.println("doc in cache");
            return doc;
        } else { // if doc not in cache check database
            System.out.println("doc not in cache");
            doc = checkDatabase(serverID);
        }
        if (doc != null) { // if doc in database add doc to cache and return doc
            System.out.println("doc in database");
            addDocToCache(serverID, doc);
            return doc;
        } else if (mongoOK) { // if doc not retrieved from database check if mongo is ok
            System.out.println("mongok status: " + mongoOK);
            System.out.println("adding doc to database and cache");
            // if mongo is ok create a new doc in database and add to cache
            doc = createNewDoc(serverID, null);
            addDocToCache(serverID, doc);
            return doc;
        } else {
            throw new DocumentUnavailableException("Database unreachable and server not in cache.");
        }
    }
    private static Document checkCache(long userID) {
        return serverCache.get(userID);
    }
    private static Document checkDatabase(long userID) {
        Document document = mongoOK ? mongoServerCollection.find(eq("_id", userID)).first() : null;
        if (document.getInteger("documentVersion") != currentDocumentVersion) {
            document = createNewDoc(document.getLong("_id"), document);
        }
        return document;
    }
    @SuppressWarnings("unchecked")
    private static Document createNewDoc(long serverID, Document oldDoc) {
        oldDoc = oldDoc != null ? oldDoc : new Document();
        return new Document()
            .append("_id", serverID)
            .append("documentVersion", currentDocumentVersion)
            .append("lastCommand", Instant.now().getEpochSecond()/60)
            .append("muteRoleID", (Long) oldDoc.getOrDefault("muteRoleID", null))
            .append("modLogEnabled", oldDoc.getBoolean("modLogEnabled", false)) // mod log stuff
            .append("logChannelID", (Long) oldDoc.getOrDefault("logChannelID", null))
            .append("logBans", oldDoc.getBoolean("logBans", false))
            .append("logMutes", oldDoc.getBoolean("logMutes", false))
            .append("logKicks", oldDoc.getBoolean("logKicks", false))
            .append("joinMessageEnabled", oldDoc.getOrDefault("joinMessageEnabled", false))
            .append("joinMessageChannelID", oldDoc.getOrDefault("joinMessageChannelID", null))
            .append("joinMessage", (String) oldDoc.getOrDefault("joinMessage", null)) // cosmetics
            .append("joinRoleIDs", oldDoc.getList("joinRoleIDs", long.class, Collections.EMPTY_LIST));
    }
    private static void addDocToCache(long userID, Document document) {
        serverCache.put(userID, document);
    }

    private static transient final ReplaceOptions replaceOpts = new ReplaceOptions().upsert(true);
    private static transient final BulkWriteOptions bulkOpts = new BulkWriteOptions().ordered(false);
    private static void syncCacheToDatabase() {
        HashMap<Long, Document> cacheCopy = new HashMap<Long, Document>(serverCache);
        ArrayList<ReplaceOneModel<Document>> writeReqs = new ArrayList<ReplaceOneModel<Document>>(cacheCopy.size());
        long currentMinute = Instant.now().getEpochSecond()/60;
        for (Map.Entry<Long, Document> entry : cacheCopy.entrySet()) {
            writeReqs.add(new ReplaceOneModel<Document>(eq("_id", entry.getKey()), entry.getValue(), replaceOpts));
            if (currentMinute - entry.getValue().get("lastCommand", Instant.now().getEpochSecond()/60) > 30) { //removes doc from cache if they havent used a cmd in 30 mins
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
        System.out.println("cache synced");
    }

    public static void removeServer(long serverID) {
        serverCache.remove(serverID);
        mongoServerCollection.deleteOne(eq("_id", serverID));
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

        public static void sendStandardResponse(SlashCommandInteraction interaction) {
            interaction.createImmediateResponder().setContent("Database is currently unreachable and server is not cached.").respond();
        }
    }
}