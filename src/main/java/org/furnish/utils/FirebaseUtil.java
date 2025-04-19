package org.furnish.utils;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.firestore.WriteResult;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class FirebaseUtil {
    private static final String API_KEY = "AIzaSyASyOWcJHKrLduqjQxvA80FtOqpjSsbDKU";
    private static final String SIGN_UP_URL = "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key="
            + API_KEY;
    private static final String SIGN_IN_URL = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key="
            + API_KEY;
    private static Firestore firestore;
    private static final Executor executor;

    static {
        executor = Executors.newFixedThreadPool(2);
    }

    public static void initializeFirebase() {
        try {
            // Debug resource loading
            System.out.println("ClassLoader: " + FirebaseUtil.class.getClassLoader());
            System.out.println("Resource path: firebase-service-account.json");
            System.out.println("Resource URL: "
                    + FirebaseUtil.class.getClassLoader().getResource("firebase-service-account.json"));

            // Load service account file
            InputStream serviceAccount = FirebaseUtil.class.getClassLoader()
                    .getResourceAsStream("firebase-service-account.json");
            if (serviceAccount == null) {
                throw new IOException(
                        "Firebase service account file not found at 'firebase-service-account.json'. Ensure it is in src/main/resources/");
            }

            // Initialize Firebase Authentication
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                System.out.println("Firebase Authentication initialized successfully");
            }

            // Initialize Firestore
            serviceAccount = FirebaseUtil.class.getClassLoader()
                    .getResourceAsStream("firebase-service-account.json");
            if (serviceAccount == null) {
                throw new IOException("Firebase service account file not found for Firestore initialization");
            }

            FirestoreOptions firestoreOptions = FirestoreOptions.newBuilder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();
            firestore = firestoreOptions.getService();
            System.out.println("Firestore initialized successfully");
        } catch (IOException e) {
            System.err.println("Failed to initialize Firebase: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Unexpected error initializing Firebase: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static JSONObject signUp(String email, String password) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        JSONObject requestBody = new JSONObject()
                .put("email", email)
                .put("password", password)
                .put("returnSecureToken", true);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SIGN_UP_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return new JSONObject(response.body());
    }

    public static JSONObject signIn(String email, String password) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        JSONObject requestBody = new JSONObject()
                .put("email", email)
                .put("password", password)
                .put("returnSecureToken", true);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SIGN_IN_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return new JSONObject(response.body());
    }

    public static void saveUserData(String uid, String name, String username, String email, String role) {
        if (firestore == null) {
            System.err.println(
                    "Cannot save user data: Firestore is not initialized. Check Firebase initialization logs.");
            return;
        }

        Map<String, Object> userData = new HashMap<>();
        userData.put("name", name);
        userData.put("username", username);
        userData.put("email", email);
        userData.put("role", role);

        try {
            // Perform the write operation and block until completion
            WriteResult result = firestore.collection("users").document(uid).set(userData).get();
            System.out.println("User data saved successfully at " + result.getUpdateTime());
        } catch (Exception e) {
            System.err.println("Failed to save user data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void testFirestore() {
        if (firestore == null) {
            System.err.println("Firestore is not initialized. Check initialization logs for errors.");
            return;
        }
        try {
            Map<String, Object> testData = new HashMap<>();
            testData.put("test", "value");
            WriteResult result = firestore.collection("test").document("testDoc").set(testData).get();
            System.out.println("Firestore test write successful at " + result.getUpdateTime());
        } catch (Exception e) {
            System.err.println("Firestore test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}