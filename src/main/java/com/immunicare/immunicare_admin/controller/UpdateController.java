package com.immunicare.immunicare_admin.controller;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Key;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.immunicare.immunicare_admin.database.adapter;
import com.immunicare.immunicare_admin.params.ClientAccounts;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.scene.control.Button;

@Controller
public class UpdateController implements Initializable {

    @Value("${encryptionKey}")
    private String encryptionKey;

    @Value("${bucketName}")
    private String bucketName;

    private String clientIdentifier;
    private Firestore db;
    private Storage storage;
    public FileChooser fileSelector;
    @FXML
    private ComboBox searchFilter;
    @FXML
    private TableView<ClientAccounts> accountsView;
    @FXML
    private TableColumn<ClientAccounts, String> usernameColumn;

    @FXML
    private TableColumn<ClientAccounts, String> firstnameColumn;

    @FXML
    private TableColumn<ClientAccounts, String> lastnameColumn;

    @FXML
    private TableColumn<ClientAccounts, String> clientIdentifierColumn;

    @FXML
    private TableColumn<ClientAccounts, String> imageURLColumn;

    @FXML
    private TableColumn<ClientAccounts, String> accountStatusColumn;

    @FXML
    private TextField username;
    @FXML
    private TextField lastName;
    @FXML
    private TextField firstName;
    @FXML
    private PasswordField password;
    @FXML
    private TextField searchText;
    @FXML
    private Label signatureImage;
    @FXML
    private Button updateButton;
    private String selectedImagePath, imageExpirationDate;

    private void initData() {
        db = adapter.getFirestore();
        storage = adapter.getStorage();
    }

    private static String encrypt(String strToEncrypt, String secret) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            byte[] key = sha.digest(secret.getBytes("UTF-8"));
            key = Arrays.copyOf(key, 16);
            Key secretKey = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);

            return bytesToHex(cipher.doFinal(strToEncrypt.getBytes("UTF-8")));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public void initializeTableModel() {
        CollectionReference collection = db.collection("clients");
        Query query = collection;
        ApiFuture<QuerySnapshot> querySnapshot = query.get();
        Executors.newSingleThreadExecutor().submit(() -> {
            try {

                QuerySnapshot snapshot = querySnapshot.get();
                ObservableList<ClientAccounts> data = FXCollections.observableArrayList();

                for (DocumentSnapshot document : snapshot.getDocuments()) {
                    String username = document.getString("username");
                    String firstname = document.getString("firstName");
                    String lastname = document.getString("lastName");
                    String clientIdentifier = document.getString("clientIdentifier");
                    String imageURL = document.getString("imageUrl");
                    String accountStatus = document.getString("accountStatus");

                    data.add(new ClientAccounts(username, firstname, lastname, clientIdentifier, imageURL, accountStatus));
                }

                Platform.runLater(() -> {
                    usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
                    firstnameColumn.setCellValueFactory(new PropertyValueFactory<>("firstname"));
                    lastnameColumn.setCellValueFactory(new PropertyValueFactory<>("lastname"));
                    clientIdentifierColumn.setCellValueFactory(new PropertyValueFactory<>("clientIdentifier"));
                    imageURLColumn.setCellValueFactory(new PropertyValueFactory<>("imageURL"));
                    accountStatusColumn.setCellValueFactory(new PropertyValueFactory<>("accountStatus"));

                    accountsView.setItems(data);
                    setRowFactory(accountsView);
                    accountsView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
                        if (newSelection != null) {
                            handleAccountSelection(newSelection);
                        }
                    });
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initData();
        username.setDisable(true);
        firstName.setDisable(true);
        lastName.setDisable(true);
        ObservableList<String> options = FXCollections.observableArrayList("First Name", "Last Name", "Username");
        searchFilter.setItems(options);
        initializeTableModel();
        addSearchTextListener();

    }

    private void handleAccountSelection(ClientAccounts selectedAccount) {
        username.setText(selectedAccount.getUsername());
        firstName.setText(selectedAccount.getFirstname());
        lastName.setText(selectedAccount.getLastname());
        clientIdentifier = selectedAccount.getClientIdentifier();
        try {
            Image image = new Image(selectedAccount.getImageURL());
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(signatureImage.getWidth());
            imageView.setFitHeight(signatureImage.getHeight());
            signatureImage.setGraphic(imageView);
            signatureImage.setText("");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void search(String filterValue, String searchValue) {
        try {
            CollectionReference collection = db.collection("clients");
            Query query;
            if (searchValue == null || searchValue.isEmpty()) {
                query = collection;
            } else {
                query = collection.whereEqualTo(filterValue, searchValue);
            }

            ApiFuture<QuerySnapshot> querySnapshot = query.get();
            ObservableList<ClientAccounts> data = FXCollections.observableArrayList();
            for (DocumentSnapshot document : querySnapshot.get().getDocuments()) {
                String username = document.getString("username");
                String firstname = document.getString("firstName");
                String lastname = document.getString("lastName");
                String clientIdentifier = document.getString("clientIdentifier");
                String imageURL = document.getString("imageUrl");
                String accountStatus = document.getString("accountStatus");
                data.add(new ClientAccounts(username, firstname, lastname, clientIdentifier, imageURL, accountStatus));
            }

            accountsView.setItems(data);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addSearchTextListener() {
        searchText.textProperty().addListener((observable, oldValue, newValue) -> {
            final String filterValue = (String) searchFilter.getSelectionModel().getSelectedItem();
            final String searchValue = searchText.getText().trim();
            final String firebaseField;
            firebaseField = switch (filterValue) {
                case "First Name" ->
                    "firstName";
                case "Last Name" ->
                    "lastName";
                case "Username" ->
                    "username";
                default ->
                    "";
            };
            Task<Void> task = new Task<Void>() {
                @Override
                protected Void call() {
                    search(firebaseField, searchValue);
                    return null;
                }
            };
            new Thread(task).start();
        });
    }

    private String uploadImageToStorage(String imagePath, String clientIdentifier) {
        try {
            String destinationFileName = "Images/Clients/" + clientIdentifier + ".png";
            BlobId blobId = BlobId.of(bucketName, destinationFileName);
            Blob blob = storage.get(blobId);
            if (blob != null) {
                blob.delete();
            }

            byte[] fileContent = Files.readAllBytes(Paths.get(imagePath));
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                    .setContentType("image/png")
                    .build();
            storage.create(blobInfo, fileContent);
            Date expiration = new Date(System.currentTimeMillis() + 365 * 24 * 60 * 60 * 1000L);
            URL signedUrl = storage.signUrl(blobInfo, expiration.getTime(), TimeUnit.MILLISECONDS, Storage.SignUrlOption.withV2Signature());
            Date signedUrlExpiration = new Date(expiration.getTime());
            imageExpirationDate = signedUrlExpiration.toString();
            return signedUrl.toString();

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getStatusForClient(String clientID) {
        try {
            ApiFuture<QuerySnapshot> querySnapshotFuture = db.collection("clients").whereEqualTo("clientIdentifier", clientID).get();
            String accountStatus = null;
            QuerySnapshot querySnapshot = querySnapshotFuture.get();
            for (QueryDocumentSnapshot document : querySnapshot.getDocuments()) {
                accountStatus = document.getString("accountStatus");

            }
            return accountStatus;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void updateTableModel() {
        try {
            CollectionReference collection = db.collection("clients");
            Query query = collection;

            ApiFuture<QuerySnapshot> querySnapshot = query.get();
            ObservableList<ClientAccounts> list = FXCollections.observableArrayList();

            for (DocumentSnapshot document : querySnapshot.get().getDocuments()) {
                String username = document.getString("username");
                String firstname = document.getString("firstName");
                String lastname = document.getString("lastName");
                String clientIdentifier = document.getString("clientIdentifier");
                String imageURL = document.getString("imageUrl");
                String accountStatus = document.getString("accountStatus");

                ClientAccounts clientAccounts = new ClientAccounts(username, firstname, lastname, clientIdentifier, imageURL, accountStatus);
                list.add(clientAccounts);

            }

            accountsView.setItems(list);
            setRowFactory(accountsView);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setRowFactory(TableView<ClientAccounts> tableView) {
        tableView.setRowFactory(tv -> {
            TableRow<ClientAccounts> row = new TableRow<>();
            row.itemProperty().addListener((obs, oldValue, newValue) -> {
                if (newValue != null) {
                    if (newValue.getAccountStatus().equals("INACTIVE")) {
                        row.setStyle("-fx-background-color: red");
                    } else {
                        row.setStyle("");
                    }
                }
            });
            return row;
        });
    }

    @FXML
    private void disable(ActionEvent event) {
        try {
            if (firstName.getText() == null || lastName.getText() == null || username.getText() == null || clientIdentifier == null) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Warning");
                alert.setContentText("Please select an Account first from the Table");
                alert.showAndWait();
            } else {
                ApiFuture<QuerySnapshot> querySnapshotFuture = db.collection("clients").whereEqualTo("clientIdentifier", clientIdentifier).get();
                try {
                    QuerySnapshot querySnapshot = querySnapshotFuture.get();
                    for (QueryDocumentSnapshot document : querySnapshot.getDocuments()) {
                        String documentId = document.getId();
                        String currentStatus = document.getString("accountStatus");
                        if ("INACTIVE".equals(currentStatus)) {
                            Alert alert = new Alert(Alert.AlertType.WARNING);
                            alert.setTitle("Warning");
                            alert.setContentText("Account is already disabled");
                            alert.showAndWait();
                        } else {
                            DocumentReference documentRef = db.collection("clients").document(documentId);
                            Map<String, Object> updates = new HashMap<>();
                            updates.put("accountStatus", "INACTIVE");
                            documentRef.update(updates).get();
                            Alert alert = new Alert(Alert.AlertType.INFORMATION);
                            alert.setTitle("Account Updated");
                            alert.setContentText("Account has been Disabled");
                            alert.showAndWait();
                            updateTableModel();
                        }
                    }
                } catch (Exception x) {
                    x.printStackTrace();
                }
            }
        } catch (Exception y) {
            y.printStackTrace();
        }
    }

    @FXML
    private void addPhoto(ActionEvent event) {
        String status = getStatusForClient(clientIdentifier);
        if (firstName.getText() == null || lastName.getText() == null || username.getText() == null || clientIdentifier == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Warning");
            alert.setContentText("Please select an Account first from the Table");
            alert.showAndWait();
            return;
        }
        if ("INACTIVE".equals(status)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Warning");
            alert.setContentText("Account is set to INACTIVE, and no longer be Reactivated and used.\nPlease contact the administrator for further assistance.");
            alert.showAndWait();
            return;
        }
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter("Image Files", "*.jpg", "*.gif", "*.png", "*.jpeg");
        fileChooser.getExtensionFilters().add(filter);
        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            selectedImagePath = selectedFile.getAbsolutePath();
            try {
                Image image = new Image(selectedFile.toURI().toURL().toString());
                ImageView imageView = new ImageView(image);
                imageView.setFitWidth(signatureImage.getWidth());
                imageView.setFitHeight(signatureImage.getHeight());
                signatureImage.setGraphic(imageView);
                signatureImage.setText("");

            } catch (MalformedURLException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Invalid Image");
                alert.setContentText("Error loading image: \n"
                        + e.getMessage());
                alert.showAndWait();
            }
        }
    }

    @FXML
    private void updateData(ActionEvent event) {
        String status = getStatusForClient(clientIdentifier);
        if ("INACTIVE".equals(status)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Warning");
            alert.setContentText("Account is inactive. Changes will not be implemented.");
            alert.showAndWait();
            return;
        }
        if (firstName.getText() == null || lastName.getText() == null || username.getText() == null || clientIdentifier == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Warning");
            alert.setContentText("Please select an Account first from the Table");
            alert.showAndWait();
            return;
        }
        if (selectedImagePath != null || password.getText() != null) {
            try {
                update(selectedImagePath, password.getText(), clientIdentifier);
            } catch (FileNotFoundException e1) {
                e1.printStackTrace();
            }
        }
    }

    public void update(String imagePath, String password, String clientID) throws FileNotFoundException {
        updateButton.setDisable(true);

        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() {
                try {
                    ApiFuture<QuerySnapshot> querySnapshotFuture = db.collection("clients").whereEqualTo("clientIdentifier", clientIdentifier).get();
                    QuerySnapshot querySnapshot = querySnapshotFuture.get();
                    for (QueryDocumentSnapshot document : querySnapshot.getDocuments()) {
                        String documentId = document.getId();
                        DocumentReference documentRef = db.collection("clients").document(documentId);
                        if (password != null && !password.trim().isEmpty()) {
                            try {
                                String encryptedEnteredPassword = encrypt(password, encryptionKey);
                                Map<String, Object> updates = new HashMap<>();
                                updates.put("password", encryptedEnteredPassword);
                                updates.put("temporaryPassword", encryptedEnteredPassword);

                                if (imagePath != null && !imagePath.trim().isEmpty()) {
                                    updates.put("imageUrl", uploadImageToStorage(imagePath, clientID));
                                    updates.put("signatureExpiration", imageExpirationDate);
                                }

                                documentRef.update(updates).get();
                                Platform.runLater(() -> {
                                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                                    if (imagePath != null && !imagePath.trim().isEmpty()) {
                                        alert.setContentText("Account and signature have been Updated");
                                    } else {
                                        alert.setContentText("Password has been Updated");
                                    }
                                    alert.setTitle("Account Updated");
                                    alert.showAndWait();
                                    Clear();
                                });
                                return null;
                            } catch (Exception e) {
                                handleException("Error updating account", e);
                            }
                        } else if (imagePath != null && !imagePath.trim().isEmpty()) {
                            try {
                                Map<String, Object> updates = new HashMap<>();
                                updates.put("imageUrl", uploadImageToStorage(imagePath, clientID));
                                updates.put("signatureExpiration", imageExpirationDate);
                                documentRef.update(updates).get();
                                Platform.runLater(() -> {
                                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                                    alert.setTitle("Account Updated");
                                    alert.setContentText("Signature has been Updated");
                                    alert.showAndWait();
                                    Clear();
                                });
                                return null;
                            } catch (Exception e) {
                                handleException("Error updating signature", e);
                            }
                        } else {
                            Platform.runLater(() -> {
                                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                                alert.setTitle("Account Updated");
                                alert.setContentText("No updates specified");
                                alert.showAndWait();
                            });
                        }
                    }
                } catch (Exception e) {
                    handleException("An error occurred", e);
                }
                return null;
            }
        };
        task.setOnSucceeded(event -> {
            updateButton.setDisable(false);
        });
        task.setOnFailed(event -> {
            updateButton.setDisable(false);
        });
        new Thread(task).start();
    }

    private void handleException(String message, Exception e) {
        e.printStackTrace();

    }

    public void Clear() {
        firstName.setText("");
        lastName.setText("");
        signatureImage.setText("No Image");
        signatureImage.setGraphic(null);
        password.setText("");
        username.setText("");
        accountsView.getSelectionModel().clearSelection();
    }

}
