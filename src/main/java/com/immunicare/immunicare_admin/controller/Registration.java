package com.immunicare.immunicare_admin.controller;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Key;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.immunicare.immunicare_admin.database.adapter;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.FileChooser;

@Controller
public class Registration implements Initializable {

    @Value("${bucketName}")
    private String bucketName;

    @Value("${encryptionKey}")
    private String encryptionKey;

    @FXML
    private TextField firstName;
    @FXML
    private Label imageSignature;
    @FXML
    private TextField lastName;
    @FXML
    private TextField licenseNumber;
    @FXML
    private ChoiceBox titleChoice;
    private String selectedImagePath, imageExpirationDate;

    private Firestore db;
    private Storage storage;

    @FXML
    public void CreateAccount(ActionEvent event) {

        String titleChoiceText = (String) titleChoice.getValue();
        String licenseNumberText = licenseNumber.getText();
        String randomString = generateRandomString();
        String password = generatePassword();
        String encryptedEnteredPassword = encrypt(password, encryptionKey);
        String clientIdentifier = clientIdentifier();
        String clientFirstName = firstName.getText().toString();
        String clientLastName = lastName.getText().toString();
        String licenseNumberPattern = "\\b\\d{7}\\b";
        if (selectedImagePath != null && !selectedImagePath.isEmpty() && !clientFirstName.isEmpty() && !clientLastName.isEmpty() && (!licenseNumberText.isEmpty() && licenseNumberText.matches(licenseNumberPattern) && !titleChoiceText.isEmpty())) {
            addDataToFirestore(uploadImageToStorage(selectedImagePath, clientIdentifier), randomString, encryptedEnteredPassword, clientIdentifier, titleChoiceText, licenseNumberText);
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Account Created");
            alert.setHeaderText("Client Account Created");
            alert.setContentText("Please be advised credentials are \n Username:" + randomString + "\n Password:" + password + "\n Please Advise Client to Reset Password Immediately, Account Credentials has been copied, Kindly Paste it to any Text Editor");
            alert.showAndWait();
            copyToClipboard("Username: " + randomString + "" + "Password: " + password);
            Clear();
        } else if (selectedImagePath == null || selectedImagePath.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("No image selected");
            alert.showAndWait();
        } else if (clientFirstName.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Enter Client First Name");
            alert.showAndWait();
        } else if (clientLastName.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Enter Client Last Name");
            alert.showAndWait();
        } else if (titleChoiceText.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Enter Medical Practitioner Title");
            alert.showAndWait();
        } else if (licenseNumberText.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Enter License Number");
            alert.showAndWait();
        } else if (!licenseNumberText.matches(licenseNumberPattern)) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Invalid License Number Format");
            alert.showAndWait();
        }

    }

    private void initData() {
        db = adapter.getFirestore();
        storage = adapter.getStorage();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initData();
        ObservableList<String> options = FXCollections.observableArrayList("Doctor", "Nurse");
        titleChoice.setItems(options);

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
            URL signedUrl = storage.signUrl(
                    blobInfo, expiration.getTime(), TimeUnit.MILLISECONDS, Storage.SignUrlOption.withV2Signature());
            Date signedUrlExpiration = new Date(expiration.getTime());
            imageExpirationDate = signedUrlExpiration.toString();
            return signedUrl.toString();

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void addDataToFirestore(String imageUrl, String username, String password, String clientIdentifier, String title, String license_number) {
        try {
            String clientFirstName = firstName.getText().toString();
            String clientLastName = lastName.getText().toString();
            Map<String, Object> data = new HashMap<>();
            data.put("imageUrl", imageUrl);
            data.put("username", username);
            data.put("password", password);
            data.put("firstName", clientFirstName);
            data.put("lastName", clientLastName);
            data.put("clientIdentifier", clientIdentifier);
            data.put("accountStatus", "ACTIVE");
            data.put("temporaryPassword", password);
            data.put("title", title);
            data.put("license_number", license_number);
            data.put("signatureExpiration", imageExpirationDate);

            db.collection("clients").add(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    public static String clientIdentifier() {
        final String SPECIAL_CHARACTERS = "!@#$%^&*()_-+=<>?/{}[]";
        final String LETTERS = "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNOPQRSTUVWXYZ";
        final String NUMBERS = "0123456789";
        StringBuilder clientIdentifier = new StringBuilder();
        SecureRandom random = new SecureRandom();
        clientIdentifier.append(SPECIAL_CHARACTERS.charAt(random.nextInt(SPECIAL_CHARACTERS.length())));
        clientIdentifier.append(NUMBERS.charAt(random.nextInt(NUMBERS.length())));
        for (int i = 0; i < 20; i++) {
            clientIdentifier.append(LETTERS.charAt(random.nextInt(LETTERS.length())));
        }

        return clientIdentifier.toString();
    }

    public static String generateRandomString() {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz0123456789";
        StringBuilder randomString = new StringBuilder();

        Random random = new Random();
        for (int i = 0; i < 5; i++) {
            int index = random.nextInt(characters.length());
            randomString.append(characters.charAt(index));
        }

        return "IM-" + randomString.toString();
    }

    public static String generatePassword() {
        final String SPECIAL_CHARACTERS = "!@#$%^&*()_-+=<>?/{}[]";
        final String LETTERS = "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNOPQRSTUVWXYZ";
        final String NUMBERS = "0123456789";
        StringBuilder password = new StringBuilder();
        SecureRandom random = new SecureRandom();
        password.append(SPECIAL_CHARACTERS.charAt(random.nextInt(SPECIAL_CHARACTERS.length())));
        password.append(NUMBERS.charAt(random.nextInt(NUMBERS.length())));
        for (int i = 0; i < 8; i++) {
            password.append(LETTERS.charAt(random.nextInt(LETTERS.length())));
        }

        return password.toString();
    }

    public void ResizeImage(String imgPath) {
        try {
            File file = new File(imgPath);
            URL url = file.toURI().toURL();
            BufferedImage awtImage = ImageIO.read(url);
            Image fxImage = SwingFXUtils.toFXImage(awtImage, null);
            ImageView imageView = new ImageView(fxImage);
            imageView.setFitWidth(imageSignature.getWidth());
            imageView.setFitHeight(imageSignature.getHeight());
            imageSignature.setGraphic(imageView);
            imageSignature.setText("");
        } catch (IOException e) {
            System.err.println("Error reading image file: " + e.getMessage());
        }
    }

    public void Clear() {
        firstName.setText("");
        lastName.setText("");
        imageSignature.setText("No Image");
        imageSignature.setGraphic(null);
        titleChoice.setValue("");
        licenseNumber.setText("");
    }

    public static void copyToClipboard(String text) {
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(text);
        clipboard.setContent(content);
    }

    @FXML
    public void Upload(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter("Image Files", "*.jpg", "*.gif", "*.png");
        fileChooser.getExtensionFilters().add(filter);
        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            selectedImagePath = selectedFile.getAbsolutePath();
            ResizeImage(selectedImagePath);

        }
    }
}
