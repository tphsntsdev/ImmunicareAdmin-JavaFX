package com.immunicare.immunicare_admin.controller;

import java.net.URL;
import java.security.Key;
import java.security.MessageDigest;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.immunicare.immunicare_admin.config.StageManager;
import com.immunicare.immunicare_admin.database.adapter;
import com.immunicare.immunicare_admin.session.JavaSession;
import com.immunicare.immunicare_admin.view.FXMLView;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;

@Controller

public class LoginController implements Initializable {

    private Firestore db;

    @Value("${encryptionKey}")
    private String encryptionKey;

    @FXML
    private Button btnLogin;
    @FXML
    private ImageView imgLogo;
    @FXML
    private PasswordField password;

    @FXML
    private TextField username;

    @FXML
    private Label lblLogin;

    @FXML
    private Hyperlink lblforgotpassword;

    @Lazy
    @Autowired
    private StageManager stageManager;

    private void initData() {
        db = adapter.getFirestore();
    }

    @FXML
    public void authenticateUser(ActionEvent event) {
        try {
            String getUsername = username.getText();
            String getPassword = password.getText();
            String encryptedEnteredPassword = encrypt(getPassword, encryptionKey);
            String decryptedEnteredPassword = decrypt(encryptedEnteredPassword, encryptionKey);

            if (getUsername.equals("Admin-IM") && decryptedEnteredPassword.equals("F*Z82ZQTS6XnwpFRXcm1")) {
                adapter.getApp();
                FXMLView nextView = FXMLView.ADMIN;
                stageManager.switchScene(nextView);
            } else {
                Query query = db.collection("clients").whereEqualTo("username", getUsername);
                ApiFuture<QuerySnapshot> querySnapshotFuture = query.get();
                QuerySnapshot querySnapshot = querySnapshotFuture.get();
                if (!querySnapshot.isEmpty()) {
                    for (DocumentSnapshot userDoc : querySnapshot.getDocuments()) {
                        String accountStatus = userDoc.getString("accountStatus");
                        String firebaseEncryptedPassword = userDoc.getString("password");
                        String clientFirstName = userDoc.getString("firstName");
                        String clientLastName = userDoc.getString("lastName");
                        String clientSignature = userDoc.getString("imageUrl");
                        String signatureExpiration = userDoc.getString("signatureExpiration");
                        String clientTitle = userDoc.getString("title");
                        String clientLicenseNumber = userDoc.getString("license_number");
                        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");
                        Date parsedDate = dateFormat.parse(signatureExpiration);
                        if (accountStatus.equals("ACTIVE")) {
                            if (!userDoc.contains("temporaryPassword")) {
                                String decryptedStoredPassword = decrypt(firebaseEncryptedPassword, encryptionKey);
                                if (decryptedStoredPassword.equals(getPassword)) {
                                    if (!isNow(parsedDate)) {
                                        adapter.getApp();
                                        JavaSession.getInstance().startSession(clientFirstName, clientLastName, clientSignature, clientTitle, clientLicenseNumber);
                                        FXMLView nextView = FXMLView.CLIENT;
                                        stageManager.switchScene(nextView);

                                    } else {
                                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                                        alert.setTitle("Signature Expiration");
                                        alert.setContentText("Please contact your Admin to update your Signature.\n"
                                                + "This is required to proceed with the application.");
                                        alert.showAndWait();
                                    }
                                } else {
                                    Alert alert = new Alert(Alert.AlertType.ERROR);
                                    alert.setTitle("Invalid Credentials");
                                    alert.setContentText("Invalid Credentials, Please Try Again.\n"
                                            + "Make sure your username and password are correct.");
                                    alert.showAndWait();
                                }
                            } else {
                                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                                alert.setTitle("Temporary Password");
                                alert.setContentText("Please Change your Password, by clicking Forgot your Password.\n"
                                        + "This is a temporary password and needs to be changed for security reasons.");
                                alert.showAndWait();
                            }
                        } else {
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setTitle("Account Disabled");
                            alert.setContentText("Your Account is Disabled, please contact your Admin.\n"
                                    + "They will be able to assist you in enabling your account.");
                            alert.showAndWait();
                        }

                    }
                } else {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Invalid Credentials");
                    alert.setContentText("Invalid Credentials");
                    alert.showAndWait();
                }
            }
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    @FXML
    public void forgotPassword() {
        FXMLView nextView = FXMLView.FORGOTPASSWORD;
        stageManager.switchScene(nextView);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initData();
        updateExpiredDocuments();
        lblforgotpassword.setOnMouseClicked(event -> {
            forgotPassword();
        });
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

    private static String decrypt(String strToDecrypt, String secret) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            byte[] key = sha.digest(secret.getBytes("UTF-8"));
            key = Arrays.copyOf(key, 16);

            Key secretKey = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            return new String(cipher.doFinal(hexToBytes(strToDecrypt)), "UTF-8");
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

    private static byte[] hexToBytes(String hex) {
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length(); i += 2) {
            bytes[i / 2] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
        }
        return bytes;
    }

    public void updateExpiredDocuments() {
        try {
            DateFormat dateFormat = new SimpleDateFormat("M-d-yyyy");
            Date currentDate = new Date();
            ApiFuture<QuerySnapshot> querySnapshot = db.collection("newsUpdate").get();
            for (QueryDocumentSnapshot document : querySnapshot.get().getDocuments()) {
                String validityDateString = document.getString("validityDate");
                if (validityDateString != null) {
                    Date validityDate = dateFormat.parse(validityDateString);
                    if (validityDate != null && validityDate.before(currentDate)) {
                        Map<String, Object> updateData = new HashMap<>();
                        updateData.put("status", "INACTIVE");
                        document.getReference().update(updateData);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean isNow(Date date) {
        Date currentDate = new Date();
        return date.getTime() == currentDate.getTime();
    }

}
