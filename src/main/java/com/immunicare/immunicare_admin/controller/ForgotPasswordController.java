package com.immunicare.immunicare_admin.controller;

import java.net.URL;
import java.security.Key;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.ResourceBundle;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import com.immunicare.immunicare_admin.config.StageManager;
import com.immunicare.immunicare_admin.database.adapter;
import com.immunicare.immunicare_admin.view.FXMLView;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

@Controller
public class ForgotPasswordController implements Initializable {

    @Lazy
    @Autowired
    private StageManager stageManager;

    @Value("${encryptionKey}")
    private String encryptionKey;

    private Firestore db;

    @FXML
    private TextField username;
    @FXML
    private PasswordField password;
    @FXML
    private PasswordField confirmpassword;

    private void initData() {
        db = adapter.getFirestore();
    }

    @FXML
    public void back(ActionEvent event) {
        FXMLView nextView = FXMLView.LOGIN;
        stageManager.switchScene(nextView);
    }

    @FXML
    public void changePassword(ActionEvent event) {
        try {
            String usernametxt = username.getText();
            String currentPassword = password.getText();
            String newPassword = confirmpassword.getText();
            Query query = db.collection("clients").whereEqualTo("username", usernametxt);
            ApiFuture<QuerySnapshot> querySnapshotFuture = query.get();
            QuerySnapshot querySnapshot = querySnapshotFuture.get();
            if (!querySnapshot.isEmpty()) {
                for (DocumentSnapshot userDoc : querySnapshot.getDocuments()) {
                    String storedPassword = decrypt(userDoc.getString("password"), encryptionKey);
                    if (currentPassword.equals(newPassword)) {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Error");
                        alert.setContentText("Current password and new password cannot be the same.");
                        alert.showAndWait();
                        return;
                    }
                    if (storedPassword.equals(currentPassword)) {
                        String oldPassword = userDoc.getString("password");
                        String encryptedNewPassword = encrypt(newPassword, encryptionKey);
                        if (isValidPassword(newPassword)) {
                            if (!oldPassword.equals(encryptedNewPassword)) {
                                userDoc.getReference().update("password", encryptedNewPassword);
                                userDoc.getReference().update("temporaryPassword", FieldValue.delete());
                                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                                alert.setTitle("Success");
                                alert.setContentText("Password reset successful!");
                                alert.showAndWait();
                                Clear();
                            } else {
                                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                                alert.setTitle("Info");
                                alert.setContentText("Password remains unchanged.");
                                alert.showAndWait();
                            }
                        } else {
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setTitle("Error");
                            alert.setContentText("Invalid new password. Please follow the password policy.");
                            alert.showAndWait();
                        }
                    } else {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Error");
                        alert.setContentText("Incorrect current password.");
                        alert.showAndWait();
                    }
                }
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setContentText("User not found.");
                alert.showAndWait();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    private static byte[] hexToBytes(String hex) {
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length(); i += 2) {
            bytes[i / 2] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
        }
        return bytes;
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

    private boolean isValidPassword(String password) {
        String regex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";
        return password.matches(regex);
    }

    public void Clear() {
        username.setText("");
        password.setText("");
        confirmpassword.setText("");
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initData();
    }

}
