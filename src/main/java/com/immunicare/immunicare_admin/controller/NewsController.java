package com.immunicare.immunicare_admin.controller;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import java.time.format.DateTimeFormatter;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Random;
import com.immunicare.immunicare_admin.database.adapter;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import java.util.Date;

import com.google.firebase.FirebaseApp;
import com.google.firebase.cloud.FirestoreClient;
import javafx.event.ActionEvent;
import java.time.LocalDate;
import org.springframework.stereotype.Controller;

@Controller
public class NewsController implements Initializable {

    private Firestore db;

    @FXML
    private TextArea contextText;

    @FXML
    private TextField titleText;

    @FXML
    private DatePicker dateText;

    @FXML
    private void addNews(ActionEvent event) {
        try {
            boolean hasActiveUpdates = checkForActiveUpdates();
            if (!hasActiveUpdates) {
                if (showContinueDialog()) {
                    String getTitle = titleText.getText();
                    String getContent = contextText.getText();
                    LocalDate selectedDate = dateText.getValue();

                    if (selectedDate != null && selectedDate.isBefore(LocalDate.now())) {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Error");
                        alert.setContentText("Selected date must be in the future.");
                        alert.showAndWait();
                        return;
                    }

                    String generateNewsKey = generateRandomString();
                    String getDate = "";
                    if (selectedDate != null) {
                        getDate = selectedDate.format(DateTimeFormatter.ofPattern("M-d-yyyy"));
                    }

                    Map<String, Object> data = new HashMap<>();
                    data.put("title", getTitle);
                    data.put("content", getContent);
                    data.put("validityDate", getDate);
                    data.put("newsKey", generateNewsKey);
                    data.put("status", "ACTIVE");

                    ApiFuture<DocumentReference> future = db.collection("newsUpdate").add(data);
                    DocumentReference documentReference = future.get();
                    String lastAddedDocumentId = documentReference.getId(); // declare and initialize lastAddedDocumentId
                    if (documentReference != null) {
                        documentReference.update("status", "ACTIVE");
                        setPreviousUpdatesInactive(lastAddedDocumentId); // pass lastAddedDocumentId to setPreviousUpdatesInactive
                    }

                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("News Added");
                    alert.setContentText("News has been added. It will be shown to most applications shortly.\nPlease note that only 1 active news update is allowed.");
                    alert.showAndWait();

                    clear();
                }
            } else {
                if (showUpdatesDialog()) {
                    String getTitle = titleText.getText();
                    String getContent = contextText.getText();
                    LocalDate selectedDate = dateText.getValue();

                    if (selectedDate != null && selectedDate.isBefore(LocalDate.now())) {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Error");
                        alert.setContentText("Selected date must be in the future.");
                        alert.showAndWait();
                        return;
                    }

                    String generateNewsKey = generateRandomString();
                    String getDate = "";
                    if (selectedDate != null) {
                        getDate = selectedDate.format(DateTimeFormatter.ofPattern("M-d-yyyy"));
                    }
                    Map<String, Object> data = new HashMap<>();
                    data.put("title", getTitle);
                    data.put("content", getContent);
                    data.put("validityDate", getDate);
                    data.put("newsKey", generateNewsKey);
                    data.put("status", "ACTIVE");

                    ApiFuture<DocumentReference> future = db.collection("newsUpdate").add(data);
                    DocumentReference documentReference = future.get();
                    String lastAddedDocumentId = documentReference.getId(); // declare and initialize lastAddedDocumentId
                    if (documentReference != null) {
                        documentReference.update("status", "ACTIVE");
                        setPreviousUpdatesInactive(lastAddedDocumentId); // pass lastAddedDocumentId to setPreviousUpdatesInactive
                    }

                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("News Added");
                    alert.setContentText("News has been added. It will be shown to most applications shortly.\nPlease note that only 1 active news update is allowed.");
                    alert.showAndWait();

                    clear();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initData() {
        db = adapter.getFirestore();
    }

    private boolean showContinueDialog() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("There are no active updates. Do you want to create a new update?");
        ButtonType result = alert.showAndWait().orElse(ButtonType.CANCEL);
        return result == ButtonType.OK;
    }

    private boolean showUpdatesDialog() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Do you want to Overwrite your Update?");
        ButtonType result = alert.showAndWait().orElse(ButtonType.CANCEL);
        return result == ButtonType.OK;
    }

    private void setPreviousUpdatesInactive(String lastAddedDocumentId) {
        try {
            db = adapter.getFirestore();
            QuerySnapshot querySnapshot = db.collection("newsUpdate")
                    .whereEqualTo("status", "ACTIVE")
                    .get().get();
            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                if (!document.getId().equals(lastAddedDocumentId)) { // exclude the last added document
                    db.collection("newsUpdate").document(document.getId())
                            .update("status", "INACTIVE");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean checkForActiveUpdates() {
        try {
            FirebaseApp app = adapter.getApp();
            db = FirestoreClient.getFirestore(app);
            QuerySnapshot querySnapshot = db.collection("newsUpdate")
                    .whereEqualTo("status", "ACTIVE")
                    .get().get();

            return !querySnapshot.isEmpty();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String generateRandomString() {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder randomString = new StringBuilder();

        Random random = new Random();
        for (int i = 0; i < 8; i++) {
            int index = random.nextInt(characters.length());
            randomString.append(characters.charAt(index));
        }

        return "N-" + randomString.toString();
    }

    public void updateExpiredDocuments() {
        try {
            DateFormat dateFormat = new SimpleDateFormat("M-d-yyyy");
            Date currentDate = new Date();
            ApiFuture<QuerySnapshot> querySnapshot = db.collection("newsUpdate").get();
            for (QueryDocumentSnapshot document : querySnapshot.get().getDocuments()) {
                String validityDateString = document.getString("validityDate");
                Date validityDate = dateFormat.parse(validityDateString);
                if (validityDate != null && validityDate.before(currentDate)) {
                    Map<String, Object> updateData = new HashMap<>();
                    updateData.put("status", "INACTIVE");
                    document.getReference().update(updateData);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void clear() {
        titleText.setText("");
        contextText.setText("");
        dateText.setValue(null);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initData();
        updateExpiredDocuments();

    }
}
