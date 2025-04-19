package org.furnish.utils;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
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
import java.util.concurrent.ExecutionException;

public class FirebaseUtil {
    private static final String API_KEY = "AIzaSyASyOWcJHKrLduqjQxvA80FtOqpjSsbDKU";
    private static final String SIGN_UP_URL = "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key="
            + API_KEY;
    private static final String SIGN_IN_URL = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key="
            + API_KEY;
    private static Firestore firestore;

    public static void initializeFirebase() {
        try {
            InputStream serviceAccount = FirebaseUtil.class.getClassLoader()
                    .getResourceAsStream("firebase-service-account.json");
            if (serviceAccount == null) {
                throw new IOException("Firebase service account file not found");
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                System.out.println("Firebase initialized successfully");
            }

            // Initialize Firestore
            FirestoreOptions firestoreOptions = FirestoreOptions.newBuilder()
                    .setCredentials(GoogleCredentials.fromStream(
                            FirebaseUtil.class.getClassLoader().getResourceAsStream("firebase-service-account.json")))
                    .build();
            firestore = firestoreOptions.getService();
        } catch (IOException e) {
            System.err.println("Failed to initialize Firebase: " + e.getMessage());
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

    public static void saveUserData(String uid, String name, String username, String email) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("name", name);
        userData.put("username", username);
        userData.put("email", email);

        try {
            firestore.collection("users").document(uid).set(userData).get();
            System.out.println("User data saved successfully");
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Failed to save user data: " + e.getMessage());
        }
    }
}