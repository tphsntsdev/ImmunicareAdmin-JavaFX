package com.immunicare.immunicare_admin.database;

import java.io.FileInputStream;
import java.io.IOException;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

@Component
public class adapter {

    private static adapter instance;

    private Firestore db;
    private Storage storage;
    private FirebaseApp app;

    @Value("${fileInputStream}")
    private String fileInputStream;

    @Value("${bucketID}")
    private String bucketID;

    @PostConstruct
    private void initialize() {
        instance = this; // Set the singleton instance
        initializeFirestore();
        initializeStorage();
        initializeApp();
    }

    public static Firestore getFirestore() {
        return instance.db;
    }

    public static Storage getStorage() {
        return instance.storage;
    }

    public static FirebaseApp getApp() {
        return instance.app;
    }

    private void initializeFirestore() {
        try (FileInputStream serviceAccount = new FileInputStream(fileInputStream)) {
            GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);
            FirestoreOptions firestoreOptions = FirestoreOptions.getDefaultInstance().toBuilder()
                    .setCredentials(credentials)
                    .setProjectId(bucketID)
                    .build();
            db = firestoreOptions.getService();
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize Firestore", e);
        }
    }

    private void initializeStorage() {
        try (FileInputStream serviceAccount = new FileInputStream(fileInputStream)) {
            GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);
            StorageOptions storageOptions = StorageOptions.getDefaultInstance().toBuilder()
                    .setCredentials(credentials)
                    .build();
            storage = storageOptions.getService();
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize Cloud Storage", e);
        }
    }

    private void initializeApp() {
        try (FileInputStream serviceAccount = new FileInputStream(fileInputStream)) {
            GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(credentials)
                    .setProjectId(bucketID)
                    .build();
            app = FirebaseApp.initializeApp(options);
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize Firebase app", e);
        }
    }
}
