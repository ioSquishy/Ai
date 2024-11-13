package ai.Data;

import io.github.cdimascio.dotenv.Dotenv;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
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

public class Database implements Serializable {
    private static transient final String connectionString = "mongodb+srv://squishydb:" + Dotenv.load().get("MONGO_PASS") + "@sandbox.ujgwpn6.mongodb.net/?retryWrites=true&w=majority";
    private static transient MongoClient mongoClient;
    private static transient MongoDatabase mongoDatabase;
    private static transient MongoCollection<Document> mongoServerCollection;
    public static transient boolean mongoOK = true;
    public static transient Long downUpTimeStartEpoch = Instant.now().getEpochSecond(); // record both how long the database has been up/down

    private static HashMap<Long, ServerDocument> serverCache = new HashMap<Long, ServerDocument>();

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
        if (mongoOK) {
            mongoOK = false;
            resetDownUpTime();
            saveCacheLocally();
            autoCacheExe.shutdownNow();
            autoCacheExe = Executors.newSingleThreadScheduledExecutor();
            autoCacheExe.scheduleAtFixedRate(checkMongo, 10, 10, TimeUnit.MINUTES);
        }
        App.api.getUserById("263049275196309506").join().sendMessage("MongoDB failed to connect :(");
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
            App.api.getUserById("263049275196309506").join().sendMessage("Cache did not manually save...");
        }
    }

    protected static ServerDocument getServerDoc(long serverID) throws DocumentUnavailableException {
        ServerDocument doc = serverCache.get(serverID);
        if (doc != null) { // if doc in cache return cached doc
            return doc;
        } else { // if doc not in cache check database
            doc = checkDatabase(serverID);
        }
        if (doc != null) { // if doc in database add doc to cache and return doc
            serverCache.put(serverID, doc);
            return doc;
        } else if (mongoOK) { // if doc not retrieved from database check if mongo is ok
            // if mongo is ok create a new doc in database and add to cache
            doc = new ServerDocument(serverID);
            serverCache.put(serverID, doc);
            return doc;
        } else {
            throw new DocumentUnavailableException("Database unreachable and server not in cache.");
        }
    }
    private static ServerDocument checkDatabase(long serverID) throws DocumentUnavailableException {
        Document document = mongoOK ? mongoServerCollection.find(eq("_id", serverID)).first() : null;
        return document != null ? ServerDocument.fromBsonDocument(document) : null;
    }

    private static transient final ReplaceOptions replaceOpts = new ReplaceOptions().upsert(true);
    private static transient final BulkWriteOptions bulkOpts = new BulkWriteOptions().ordered(false);
    private static void syncCacheToDatabase() {
        // create copy of cache
        HashMap<Long, ServerDocument> cacheCopy = new HashMap<Long, ServerDocument>(serverCache);
        ArrayList<ReplaceOneModel<Document>> writeReqs = new ArrayList<ReplaceOneModel<Document>>(cacheCopy.size());
        long currentMinute = Instant.now().getEpochSecond()/60;
        for (Map.Entry<Long, ServerDocument> entry : cacheCopy.entrySet()) {
            // add document from cache to write requests
            writeReqs.add(new ReplaceOneModel<Document>(eq("_id", entry.getKey()), entry.getValue().toBsonDocument(), replaceOpts));
            // remove doc from cache if they havent used a command in 30 mins
            if (currentMinute - (entry.getValue().lastCommandEpochSecond/60) > 30) { 
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
        System.out.println("Removed server: " + serverID);
        serverCache.remove(serverID);
        mongoServerCollection.deleteOne(eq("_id", serverID));
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