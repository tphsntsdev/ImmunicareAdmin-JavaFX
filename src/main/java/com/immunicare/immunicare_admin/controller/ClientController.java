package com.immunicare.immunicare_admin.controller;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Optional;

import javafx.scene.control.TextInputDialog;

import javax.imageio.ImageIO;

import java.io.InputStream;

import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Controller;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.immunicare.immunicare_admin.config.StageManager;
import com.immunicare.immunicare_admin.database.adapter;
import com.immunicare.immunicare_admin.params.AppointmentDetails;
import com.immunicare.immunicare_admin.params.SelectedAppointmentData;
import com.immunicare.immunicare_admin.params.User;
import com.immunicare.immunicare_admin.params.VaccineTakenView;
import com.immunicare.immunicare_admin.session.JavaSession;
import com.immunicare.immunicare_admin.view.FXMLView;
import com.itextpdf.io.source.ByteArrayOutputStream;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalTime;
import org.threeten.bp.format.DateTimeFormatter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.format.DateTimeParseException;

import javafx.scene.control.TextArea;

@Controller
public class ClientController implements Initializable {

    @Value("${senderEmail}")
    private String senderEmail;

    @Value("${bucketName}")
    private String bucketName;

    private Boolean switchClickedChecker = false;
    private String gbuserIdentifier = "";
    @FXML
    private ComboBox<String> userComboBox;
    @FXML
    private ListView<AppointmentDetails> appointmentsView;
    @FXML
    private ListView<VaccineTakenView> vaccineTakenTable;
    @FXML
    private TextField FirstName;
    @FXML
    private TextField LastName;
    @FXML
    private TextField Age;
    @FXML
    private TextField PhoneNumber;
    @FXML
    private TextField textSearch;
    @FXML
    private Button btnSwitch;
    @FXML
    private Button btnLogout;
    @FXML
    private Button btnUpdate;
    @FXML
    private TableView<User> userTable;
    @FXML
    private TableColumn<User, String> firstNameColumn;
    @FXML
    private TableColumn<User, String> lastNameColumn;
    @FXML
    private TableColumn<User, String> ageColumn;
    @FXML
    private TableColumn<User, String> phoneNumberColumn;
    @FXML
    private TableColumn<User, String> childNameColumn;
    @FXML
    private TableColumn<User, String> childAgeColumn;
    @FXML
    private TableColumn<User, String> childGenderColumn;
    @FXML
    private Label labelImagePane;

    private Firestore db;
    private Storage storage;
    private CollectionReference usersCollection;
    private ObservableList<User> users;
    private ObservableList<AppointmentDetails> appointmentModel;
    private ObservableList<VaccineTakenView> vaccineModel;
    public String appointmentIdentifier, adultVaccine, childVaccine, date, location, time, appointmentKeyforList;
    @Lazy
    @Autowired
    private StageManager stageManager;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        ObservableList<String> comboBoxItems = FXCollections.observableArrayList("First Name", "Last Name", "Username", "Phone Number", "Child Name");
        userComboBox.setItems(comboBoxItems);
        FirstName.setDisable(true);
        LastName.setDisable(true);
        PhoneNumber.setDisable(true);
        Age.setDisable(true);
        initData();
        initializeTableModelForUser();
        addSearchTextListener();
    }

    private void search(String firebaseField, String searchValue) {
        users.clear();
        ApiFuture<QuerySnapshot> querySnapshotApiFuture = usersCollection.whereEqualTo(firebaseField, searchValue).get();
        ApiFutures.addCallback(querySnapshotApiFuture, new ApiFutureCallback<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot querySnapshot) {
                for (DocumentSnapshot document : querySnapshot) {
                    String userId = document.getId();
                    String firstname = document.getString("firstName");
                    String lastname = document.getString("lastName");
                    String age = document.getString("age");
                    String phoneNumber = document.getString("phoneNumber");
                    String childidentifier = document.getString("childIdentifier");

                    if (document.contains("childIdentifier")) {
                        CollectionReference childrenCollection = document.getReference().collection("child");
                        ApiFuture<QuerySnapshot> childrenQuerySnapshot = childrenCollection
                                .whereEqualTo("childIdentifier", childidentifier)
                                .get();

                        ApiFutures.addCallback(childrenQuerySnapshot, new ApiFutureCallback<QuerySnapshot>() {
                            @Override
                            public void onSuccess(QuerySnapshot querySnapshot) {
                                String childName = "";
                                String childAge = "";
                                String childGender = "";
                                for (DocumentSnapshot childDocument : querySnapshot) {
                                    childName = childDocument.getString("name");
                                    childAge = childDocument.getString("age");
                                    childGender = childDocument.getString("gender");
                                }
                                User user = new User(firstname, lastname, age, phoneNumber, childName, childAge, childGender, userId);
                                users.add(user);
                                userTable.setItems(users);
                            }

                            @Override
                            public void onFailure(Throwable t) {
                                // handle exception
                                System.err.println("Error loading child data: " + t.getMessage());
                            }
                        }, Executors.newSingleThreadExecutor());
                    } else {
                        User user = new User(firstname, lastname, age, phoneNumber, "", "", "", userId);
                        users.add(user);
                        userTable.setItems(users);
                    }
                }
            }

            @Override
            public void onFailure(Throwable t) {
                // handle exception
                System.err.println("Error searching users: " + t.getMessage());
            }
        }, Executors.newSingleThreadExecutor());
    }

    private void addSearchTextListener() {
        textSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            final String filterValue = (String) userComboBox.getSelectionModel().getSelectedItem();
            final String searchValue = textSearch.getText().trim();
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

    public void ResizeImage(String imgPath) {
        try {
            URL url = new URL(imgPath);
            BufferedImage awtImage = ImageIO.read(url);
            Image fxImage = SwingFXUtils.toFXImage(awtImage, null);
            ImageView imageView = new ImageView(fxImage);
            imageView.setFitWidth(labelImagePane.getWidth());
            imageView.setFitHeight(labelImagePane.getHeight());
            labelImagePane.setGraphic(imageView);
            labelImagePane.setText("");

        } catch (IOException e) {
            System.err.println("Error reading image file: " + e.getMessage());
        }
    }

    private void handleAccountSelection(User user) {
        FirstName.setText(user.getFirstName());
        LastName.setText(user.getLastName());
        Age.setText(user.getAge());
        PhoneNumber.setText(user.getPhoneNumber());
        gbuserIdentifier = user.getUserId();
        appointmentModel = FXCollections.observableArrayList();
        appointmentsView.setItems(appointmentModel);
        Platform.runLater(() -> initializeAppointmentsView());
        vaccineModel = FXCollections.observableArrayList();
        vaccineTakenTable.setItems(vaccineModel);
        checkIdentifier(gbuserIdentifier, switchClickedChecker);
        Platform.runLater(() -> VaccineTakenView(switchClickedChecker));
        CollectionReference imageURLCollection = db.collection("users").document(gbuserIdentifier).collection("photos");
        ApiFuture<QuerySnapshot> imageQuerySnapshot = imageURLCollection.get();
        try {
            for (DocumentSnapshot imageDocument : imageQuerySnapshot.get().getDocuments()) {
                String imageURL = imageDocument.getString("photo_url");
                if (imageURL != null) {
                    ResizeImage(imageURL);
                } else {
                    labelImagePane.setText("No Image");
                    labelImagePane.setGraphic(null);
                }
            }
            if (imageQuerySnapshot.get().isEmpty()) {
                labelImagePane.setText("No Image");
                labelImagePane.setGraphic(null);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    public void checkIdentifier(String userID, Boolean isClicked) {
        DocumentReference selectedData = db.collection("users").document(userID);
        ApiFuture<DocumentSnapshot> future = selectedData.get();
        try {
            DocumentSnapshot document = future.get();
            if (document.exists()) {
                if (isClicked == false) {
                    List<?> allergyValues = (List<?>) document.get("allergy_values");
                    String allergyStatus = document.getString("allergy_status");
                    if ("Yes".equals(allergyStatus)) {
                        if (allergyValues != null) {
                            StringBuilder sb = new StringBuilder();
                            for (Object value : allergyValues) {
                                if (value != null) {
                                    sb.append(value.toString()).append("\n");
                                }
                            }

                            TextArea textArea = new TextArea(sb.toString());
                            textArea.setEditable(false);
                            Alert alert = new Alert(AlertType.INFORMATION);
                            alert.setTitle("Allergies");
                            alert.setHeaderText("List of Allergies:");
                            alert.getDialogPane().setContent(textArea);

                            alert.showAndWait();
                        }
                    }

                } else {
                    CollectionReference childCollection = db.collection("users/" + userID + "/child");
                    ApiFuture<QuerySnapshot> future1 = childCollection.get();
                    try {
                        QuerySnapshot querySnapshot = future1.get();
                        if (!querySnapshot.getDocuments().isEmpty()) {
                            for (DocumentSnapshot child_document : querySnapshot.getDocuments()) {
                                List<?> allergyValues = (List<?>) child_document.get("allergy_values");
                                String allergyStatus = child_document.getString("allergy_status");
                                if ("Yes".equals(allergyStatus)) {
                                    if (allergyValues != null) {
                                        StringBuilder sb = new StringBuilder();
                                        for (Object value : allergyValues) {
                                            if (value != null) {
                                                sb.append(value.toString()).append("\n");
                                            }
                                        }

                                        TextArea textArea = new TextArea(sb.toString());
                                        textArea.setEditable(false);
                                        Alert alert = new Alert(AlertType.INFORMATION);
                                        alert.setTitle("Child Allergies");
                                        alert.setHeaderText("List of Allergies:");
                                        alert.getDialogPane().setContent(textArea);

                                        alert.showAndWait();
                                    }
                                }

                            }
                        }
                    } catch (InterruptedException | ExecutionException e) {

                        e.printStackTrace();
                    }

                }
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

    }

    private void handleAppointmentSelection(AppointmentDetails selectedAppointment) {
        if (selectedAppointment == null) {
            appointmentIdentifier = "";
            childVaccine = "";
            adultVaccine = "";
            location = "";
            time = "";
            date = "";
            appointmentKeyforList = "";
        } else {
            switchClickedChecker = false;
            String appointmentKey = selectedAppointment.getIdentifier();
            String childVaccineString = selectedAppointment.getChildVaccine();
            String adultVaccineString = selectedAppointment.getAdultVaccine();
            String locationString = selectedAppointment.getLocation();
            String timeString = selectedAppointment.getTime();
            String dateString = selectedAppointment.getDate();
            String appointment = selectedAppointment.getAppointmentKey();
            appointmentIdentifier = appointmentKey;
            childVaccine = childVaccineString;
            adultVaccine = adultVaccineString;
            location = locationString;
            time = timeString;
            date = dateString;
            appointmentKeyforList = appointment;
            System.out.println("Key : " + appointmentKeyforList);
            System.out.println("Identifier" + appointmentIdentifier);
        }
    }

    private void initData() {
        db = adapter.getFirestore();
        storage = adapter.getStorage();

    }

    public void initializeTableModelForUser() {
        usersCollection = db.collection("users");
        users = FXCollections.observableArrayList();

        // Initialize table columns
        firstNameColumn.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        lastNameColumn.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        ageColumn.setCellValueFactory(new PropertyValueFactory<>("age"));
        phoneNumberColumn.setCellValueFactory(new PropertyValueFactory<>("phoneNumber"));
        childNameColumn.setCellValueFactory(new PropertyValueFactory<>("childName"));
        childAgeColumn.setCellValueFactory(new PropertyValueFactory<>("childAge"));
        childGenderColumn.setCellValueFactory(new PropertyValueFactory<>("childGender"));
        userTable.setItems(users);

        userTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                switchClickedChecker = false;
                handleAccountSelection(newSelection);
            }
        });

        usersCollection.addSnapshotListener((querySnapshot, e) -> {
            if (e != null) {
                System.err.println("Listen failed: " + e);
                return;
            }

            Map<String, User> newUserMap = new HashMap<>();

            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                String userId = document.getId();
                String firstname = document.getString("firstName");
                String lastname = document.getString("lastName");
                String age = document.getString("age");
                String phoneNumber = document.getString("phoneNumber");
                String childIdentifier = document.getString("childIdentifier");
                String childState = document.getString("child_state");

                DocumentReference userDocRef = document.getReference();
                CollectionReference childCollectionRef = userDocRef.collection("child");

                if (childIdentifier != null && "TRUE".equals(childState)) {
                    childCollectionRef.whereEqualTo("childIdentifier", childIdentifier)
                            .addSnapshotListener((childQuerySnapshot, childError) -> {
                                if (childError != null) {
                                    System.err.println("Child data listen failed: " + childError);
                                    return;
                                }

                                String childName = "";
                                String childAge = "";
                                String childGender = "";

                                for (DocumentSnapshot childDoc : childQuerySnapshot.getDocuments()) {
                                    childName = childDoc.getString("childName");
                                    childAge = childDoc.getString("childAge");
                                    childGender = childDoc.getString("childGender");
                                }

                                User user = new User(firstname, lastname, age, phoneNumber, childName, childAge, childGender, userId);
                                newUserMap.put(userId, user);

                                Platform.runLater(() -> users.setAll(newUserMap.values()));
                            });

                } else {
                    User user = new User(firstname, lastname, age, phoneNumber, "", "", "", userId);
                    newUserMap.put(userId, user);

                    Platform.runLater(() -> users.setAll(newUserMap.values()));
                }
            }
        });
    }
    @Autowired
    private JavaMailSender emailSender;

    public void sendEmail(final String receiverEmail, final String txtContent) {
        new Thread(() -> {
            SimpleMailMessage message = new SimpleMailMessage(); // Handle exceptions (e.g., log or notify the user)
            message.setFrom(senderEmail);
            message.setTo(receiverEmail);
            message.setSubject("Appointment Confirmation");
            message.setText(txtContent);
            emailSender.send(message);
        }).start();
    }

    public void deleteExpiredAppointments(String primaryKey, String childState, String email) {
        CollectionReference usersCollection = db.collection("users");
        ApiFuture<QuerySnapshot> querySnapshotFuture = usersCollection.whereEqualTo("pkIdentifier", primaryKey).get();

        try {
            QuerySnapshot querySnapshot = querySnapshotFuture.get();
            for (QueryDocumentSnapshot data : querySnapshot) {
                String userID = data.getId();
                CollectionReference appointmentsCollection = db.collection("users/" + userID + "/appointments");
                ApiFuture<QuerySnapshot> appointmentSnapshotFuture = appointmentsCollection.get();

                try {
                    QuerySnapshot appointmentSnapshot = appointmentSnapshotFuture.get();
                    if (!appointmentSnapshot.isEmpty()) {
                        for (QueryDocumentSnapshot appointmentData : appointmentSnapshot) {
                            String appointmentDate = Optional.ofNullable(appointmentData.getString("date")).orElse("");
                            String appointmentTime = Optional.ofNullable(appointmentData.getString("time")).orElse("");
                            String adultVaccine = Optional.ofNullable(appointmentData.getString("adultvaccine")).orElse("");
                            String appointmentKey = Optional.ofNullable(appointmentData.getString("appointmentKey")).orElse("");
                            String location = Optional.ofNullable(appointmentData.getString("location")).orElse("");
                            String appointmentDocumentID = appointmentData.getId();

                            if (!appointmentDate.isEmpty() && !appointmentTime.isEmpty()) {
                                try {
                                    String dateTime = appointmentDate + " " + appointmentTime;
                                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d/yyyy h:mm a");
                                    LocalDateTime finalDateTime = LocalDateTime.parse(dateTime, formatter);
                                    LocalDateTime now = LocalDateTime.now();

                                    if (finalDateTime.isBefore(now)) {
                                        Map<String, Object> myHashMap = new HashMap<>();
                                        myHashMap.put("adultvaccine", adultVaccine);
                                        myHashMap.put("appointmentKey", appointmentKey);
                                        myHashMap.put("date", appointmentDate);
                                        myHashMap.put("location", location);
                                        myHashMap.put("pkIdentifier", primaryKey);
                                        myHashMap.put("time", appointmentTime);
                                        myHashMap.put("status", "Expired");

                                        if ("TRUE".equals(childState)) {
                                            String childVaccine = Optional.ofNullable(appointmentData.getString("childvaccine")).orElse("");
                                            myHashMap.put("childvaccine", childVaccine);
                                        }

                                        CollectionReference deletedAppointmentsCollection = db.collection("users/" + userID + "/deletedAppointments");
                                        ApiFuture<DocumentReference> addFuture = deletedAppointmentsCollection.add(myHashMap);
                                        DocumentReference documentRef = addFuture.get();
                                        System.out.println("Document added with ID: " + documentRef.getId());

                                        DocumentReference appointmentDocRef = appointmentsCollection.document(appointmentDocumentID);
                                        ApiFuture<WriteResult> deleteFuture = appointmentDocRef.delete();
                                        deleteFuture.get(); // Wait for delete operation to complete

                                        ApiFuture<QuerySnapshot> userQuerySnapshotFuture = usersCollection.whereEqualTo("pkIdentifier", primaryKey).get();
                                        QuerySnapshot userQuerySnapshot = userQuerySnapshotFuture.get();

                                        for (QueryDocumentSnapshot x : userQuerySnapshot) {
                                            String userId = x.getId();
                                            Integer vaccinationCount = Integer.parseInt(Optional.ofNullable(x.getString("vaccination_count")).orElse("0"));
                                            Map<String, Object> appointmentList = (Map<String, Object>) x.get("appointmentList");
                                            if (appointmentList == null) {
                                                appointmentList = new HashMap<>();
                                            }
                                            vaccinationCount--;
                                            Map<String, Object> updates = new HashMap<>();
                                            updates.put("vaccination_count", vaccinationCount.toString());

                                            String keyToRemove = findKeyByValue(appointmentList, appointmentKey);
                                            System.out.println("keyToRemove: " + keyToRemove);
                                            System.out.println("List: " + appointmentList);
                                            System.out.println("DocumentID: " + appointmentDocumentID);
                                            appointmentList.remove(keyToRemove);
                                            Map<String, Object> modifiedMap = keyToRemove != null ? subtractOneFromKeys(appointmentList, keyToRemove) : appointmentList;

                                            ApiFuture<WriteResult> updateAppointmentListFuture = usersCollection.document(userId).update("appointmentList", modifiedMap);
                                            updateAppointmentListFuture.get(); // Wait for appointmentList update

                                            ApiFuture<WriteResult> updateVaccinationFuture = usersCollection.document(userId).update(updates);
                                            updateVaccinationFuture.get(); // Wait for vaccination count update

                                            String stringSample = "Your Appointment that was Scheduled on " + appointmentDate + " , " + appointmentTime + " at " + location + " has Elapsed";
                                            sendEmail(email, stringSample);
                                        }
                                    }
                                } catch (DateTimeParseException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    private void initializeAppointmentsView() {
        if (gbuserIdentifier.isEmpty()) {
            appointmentModel.clear();
            return;
        }

        try {
            DocumentReference docRef = db.collection("users").document(gbuserIdentifier);
            docRef.addSnapshotListener((documentSnapshot, e) -> {
                if (e != null) {
                    System.err.println("Error getting document: " + e.getMessage());
                    return;
                }

                if (documentSnapshot != null && documentSnapshot.exists()) {
                    Map<String, String> map = (Map<String, String>) documentSnapshot.get("appointmentList");
                    String childState = documentSnapshot.getString("child_state");
                    String pkIdentifier = documentSnapshot.getString("pkIdentifier");
                    String email = documentSnapshot.getString("email");
                    deleteExpiredAppointments(pkIdentifier, childState, email);

                    if (map != null && !map.isEmpty()) {
                        List<String> appointmentKeys = new ArrayList<>(map.values());

                        CollectionReference appointmentCollection = db.collection("users").document(gbuserIdentifier).collection("appointments");
                        Query query = appointmentCollection.whereIn("appointmentKey", appointmentKeys);

                        query.addSnapshotListener((querySnapshot, queryException) -> {
                            if (queryException != null) {
                                System.err.println("Error getting documents: " + queryException.getMessage());
                                return;
                            }

                            if (querySnapshot != null) {
                                List<AppointmentDetails> detailsList = new ArrayList<>();
                                for (DocumentSnapshot document1 : querySnapshot.getDocuments()) {

                                    String adultVaccine = document1.getString("adultvaccine");
                                    String childVaccine = document1.getString("childvaccine");
                                    String date = document1.getString("date");
                                    String time = document1.getString("time");
                                    String location = document1.getString("location");
                                    String appointmentKeyforList = document1.getString("appointmentKey");
                                    AppointmentDetails appointmentDetails = new AppointmentDetails(adultVaccine, childVaccine, date, time, location, document1.getId(), appointmentKeyforList);
                                    detailsList.add(appointmentDetails);
                                }

                                Platform.runLater(() -> {
                                    appointmentModel.setAll(detailsList);
                                });
                            }
                        });
                    } else {
                        Platform.runLater(() -> {
                            appointmentModel.clear();
                        });
                    }
                }
            });

            appointmentsView.setCellFactory(param -> new ListCell<AppointmentDetails>() {
                @Override
                protected void updateItem(AppointmentDetails item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        Text text = new Text(item.toString());
                        text.setWrappingWidth(600);
                        setGraphic(text);
                    }
                }
            });

            appointmentsView.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
                handleAppointmentSelection(newValue);
            });

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void VaccineTakenView(Boolean child_state) {
        if (gbuserIdentifier.isBlank()) {
            vaccineModel.clear();
            return;
        }

        CollectionReference appointmentCollection;
        Query query;

        if (child_state) {
            appointmentCollection = db.collection("users").document(gbuserIdentifier).collection("childCompletedAppointments");
            query = appointmentCollection.whereEqualTo("status", "Fulfilled");
        } else {
            appointmentCollection = db.collection("users").document(gbuserIdentifier).collection("adultCompletedAppointments");
            query = appointmentCollection.whereEqualTo("status", "Fulfilled");
        }

        query.addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                System.err.println("Listen failed: " + e);
                return;
            }

            Platform.runLater(() -> {
                try {
                    vaccineModel.clear();

                    if (snapshots != null && !snapshots.isEmpty()) {
                        List<VaccineTakenView> detailsList = new ArrayList<>();

                        for (DocumentSnapshot document : snapshots.getDocuments()) {
                            String adultVaccine = document.getString("adultvaccine");
                            String childVaccine = document.getString("childvaccine");
                            String date = document.getString("date");
                            String time = document.getString("time");
                            String location = document.getString("location");
                            String vaccineKey = document.getString("appointmentKey");

                            if ((child_state && !"None".equals(childVaccine) && !childVaccine.isEmpty())
                                    || (!child_state && !"None".equals(adultVaccine) && !adultVaccine.isEmpty())) {

                                VaccineTakenView vaccineDetails = new VaccineTakenView(adultVaccine, childVaccine, date, time, location, vaccineKey, child_state);
                                detailsList.add(vaccineDetails);
                            }
                        }

                        vaccineModel.setAll(detailsList);
                    } else {
                        vaccineModel.clear();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
        });

        vaccineTakenTable.setCellFactory(param -> new ListCell<VaccineTakenView>() {
            @Override
            protected void updateItem(VaccineTakenView item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    Text text = new Text(item.toString());
                    text.setWrappingWidth(600);
                    setGraphic(text);
                }
            }
        });
    }

    public void SwitchAction(ActionEvent event) {
        if (!hasSelectedIteminTable()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("No Account Selected");
            alert.setContentText("It seems that there is no Account Selected, Please select an Account");
            alert.showAndWait();
        } else {
            try {
                switchClickedChecker = !switchClickedChecker;
                DocumentReference userDocument = db.collection("users").document(gbuserIdentifier);
                vaccineModel = FXCollections.observableArrayList();
                vaccineTakenTable.setItems(vaccineModel);
                Platform.runLater(() -> VaccineTakenView(switchClickedChecker));
                ApiFuture<DocumentSnapshot> future = userDocument.get();
                DocumentSnapshot document = future.get();
                if (document.exists()) {
                    String child_statedata = document.getString("child_state");
                    if ("TRUE".equals(child_statedata) && document.contains("childIdentifier")) {
                        if (switchClickedChecker) {
                            String childName = ((TableColumn) userTable.getColumns().get(4)).getCellData(userTable.getSelectionModel().getSelectedItem()).toString();
                            String lastName = ((TableColumn) userTable.getColumns().get(1)).getCellData(userTable.getSelectionModel().getSelectedItem()).toString();
                            String childage = ((TableColumn) userTable.getColumns().get(5)).getCellData(userTable.getSelectionModel().getSelectedItem()).toString();
                            String phoneNumber = ((TableColumn) userTable.getColumns().get(3)).getCellData(userTable.getSelectionModel().getSelectedItem()).toString();
                            FirstName.setText(childName);
                            LastName.setText(lastName);
                            Age.setText(childage);
                            PhoneNumber.setText(phoneNumber);
                            CollectionReference imageURLCollection = db.collection("users").document(gbuserIdentifier).collection("child_photos");
                            ApiFuture<QuerySnapshot> imageQuerySnapshot = imageURLCollection.get();
                            checkIdentifier(gbuserIdentifier, switchClickedChecker);

                            if (!imageQuerySnapshot.get().getDocuments().isEmpty()) {
                                DocumentSnapshot imageDocument = imageQuerySnapshot.get().getDocuments().get(0);
                                String imageURL = imageDocument.getString("photo_url");
                                Platform.runLater(() -> ResizeImage(imageURL));
                            } else {
                                Platform.runLater(() -> {
                                    labelImagePane.setText("No Image");
                                    labelImagePane.setGraphic(null);
                                });
                            }

                        } else {
                            String childName = ((TableColumn) userTable.getColumns().get(0)).getCellData(userTable.getSelectionModel().getSelectedItem()).toString();
                            String lastName = ((TableColumn) userTable.getColumns().get(1)).getCellData(userTable.getSelectionModel().getSelectedItem()).toString();
                            String childage = ((TableColumn) userTable.getColumns().get(2)).getCellData(userTable.getSelectionModel().getSelectedItem()).toString();
                            String phoneNumber = ((TableColumn) userTable.getColumns().get(3)).getCellData(userTable.getSelectionModel().getSelectedItem()).toString();
                            FirstName.setText(childName);
                            LastName.setText(lastName);
                            Age.setText(childage);
                            PhoneNumber.setText(phoneNumber);
                            CollectionReference imageURLCollection = db.collection("users").document(gbuserIdentifier).collection("photos");
                            ApiFuture<QuerySnapshot> imageQuerySnapshot = imageURLCollection.get();
                            checkIdentifier(gbuserIdentifier, switchClickedChecker);
                            if (!imageQuerySnapshot.get().getDocuments().isEmpty()) {
                                DocumentSnapshot imageDocument = imageQuerySnapshot.get().getDocuments().get(0);
                                String imageURL = imageDocument.getString("photo_url");
                                Platform.runLater(() -> ResizeImage(imageURL));
                            } else {
                                Platform.runLater(() -> {
                                    labelImagePane.setText("No Image");
                                    labelImagePane.setGraphic(null);
                                });
                            }

                        }
                    } else {
                        switchClickedChecker = false;
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Error");
                        alert.setHeaderText("No Child");
                        alert.setContentText("It Seems that the Account \n selected does not contain any Child");
                        alert.showAndWait();
                    }
                } else {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText("Error");
                    alert.setContentText("Please select an Account");
                    alert.showAndWait();

                }
            } catch (InterruptedException | ExecutionException e) {
                e.getStackTrace();
            }
        }
    }

    private static void drawText(PDPageContentStream contentStream, PDType1Font font, int fontSize, String text, float x, float y) throws IOException {
        contentStream.setFont(font, fontSize);
        contentStream.beginText();
        contentStream.newLineAtOffset(x, y);
        contentStream.showText(text);
        contentStream.endText();
    }

    private static List<String> splitText(String text, PDFont font, float fontSize, float maxWidth) throws IOException {
        List<String> lines = new ArrayList<>();
        StringBuilder currentLine = new StringBuilder();
        float currentWidth = 0;

        for (char c : text.toCharArray()) {
            float charWidth = font.getStringWidth(String.valueOf(c)) / 1000 * fontSize;

            if (currentWidth + charWidth > maxWidth) {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder();
                currentWidth = 0;
            }

            currentLine.append(c);
            currentWidth += charWidth;
        }

        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        return lines;
    }

    private String findKeyByValue(Map<String, Object> map, String targetValue) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (targetValue.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    private Map<String, Object> subtractOneFromKeys(Map<String, Object> map, String keyToRemove) {
        Map<String, Object> modifiedMap = new HashMap<>();
        int keyToRemoveInt = Integer.parseInt(keyToRemove);

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            int originalKey = Integer.parseInt(entry.getKey());
            int newKey = (originalKey > keyToRemoveInt) ? originalKey - 1 : originalKey;
            modifiedMap.put(String.valueOf(newKey), entry.getValue());
        }

        return modifiedMap;
    }

    @FXML
    private void UpdateAppointment(ActionEvent event) {
        SelectedAppointmentData selectedAppointmentData = new SelectedAppointmentData();
        if (!hasSelectedIteminTable()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("No Account Selected");
            alert.setContentText("It seems that there is no Account Selected, \n Please select an Account");
            alert.showAndWait();
        } else if (!hasSelectedItem()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("No Appointment Selected");
            alert.setContentText("It seems that there are no appointment selected, \n Please select an Appointment");
            alert.showAndWait();
        } else {
            FXMLView nextView = FXMLView.UPDATEAPPOINTMENT;
            FXMLLoader loader = new FXMLLoader(getClass().getResource(nextView.getFxmlFile()));
            Parent root = null;
            try {
                root = loader.load();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Scene scene = new Scene(root);
            Stage stage = new Stage();
            stage.setTitle(nextView.getTitle());
            stage.setScene(scene);

            DocumentReference userDocument = db.collection("users").document(gbuserIdentifier);
            ApiFuture<DocumentSnapshot> future = userDocument.get();
            try {
                DocumentSnapshot document = future.get();
                if (document.exists()) {
                    String child_statedata = document.getString("child_state");
                    String childIdentifierdata = document.getString("childIdentifier");

                    if (child_statedata.equals("FALSE")) {
                        selectedAppointmentData.setAppointmentID(appointmentIdentifier);
                        selectedAppointmentData.setChildIdentifier("No Child");
                        selectedAppointmentData.setPrimaryKey(gbuserIdentifier);
                    } else {
                        selectedAppointmentData.setAppointmentID(appointmentIdentifier);
                        selectedAppointmentData.setChildIdentifier(childIdentifierdata);
                        selectedAppointmentData.setPrimaryKey(gbuserIdentifier);

                    }

                    AppointmentController controller = loader.getController();
                    controller.receiveData(selectedAppointmentData);

                }
            } catch (InterruptedException | ExecutionException e) {
                e.getStackTrace();
            }

            stage.show();
        }
    }

    @FXML
    public void Validate(ActionEvent event) {
        JavaSession javaSession = JavaSession.getInstance();
        String clientFirstname = javaSession.getfirstName();
        String clientLastname = javaSession.getLastName();
        String clientSignature = javaSession.getimageURL();
        String clientTitle = javaSession.getTitle();
        String clientLicense_Number = javaSession.getLicense_Number();

        if (!hasSelectedIteminTable()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("No Account Selected");
            alert.setContentText("It seems that there is no Account Selected, \n Please select an Account");
            alert.showAndWait();
        } else if (!hasSelectedItem()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("No Appointment Selected");
            alert.setContentText("It seems that there are no appointment selected, \n Please select an Appointment");
            alert.showAndWait();
        } else {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Confirmation");
            dialog.setHeaderText("Are you confirming that the user has been vaccinated?");
            dialog.setContentText("Type 'YES' to confirm, 'NO' to cancel:");
            Optional<String> result = dialog.showAndWait();
            if (result.isPresent()) {
                String userInput = result.get();

                if (userInput != null && userInput.equalsIgnoreCase("YES")) {
                    try {
                        AtomicReference<String> selectedchildState = new AtomicReference<>("");
                        DocumentReference childStateRef = db.collection("users").document(gbuserIdentifier);
                        ApiFuture<DocumentSnapshot> child_statefuture = childStateRef.get();
                        DocumentSnapshot documentchild_state = child_statefuture.get();
                        if (documentchild_state.exists()) {
                            selectedchildState.set(documentchild_state.getString("child_state"));
                        }
                        CollectionReference appointmentCollection = db.collection("users").document(gbuserIdentifier).collection("appointments");
                        Platform.runLater(() -> {
                            appointmentCollection.addSnapshotListener((snapshot, error) -> {
                                if (snapshot != null && !snapshot.isEmpty()) {
                                    for (DocumentSnapshot documentData : snapshot.getDocuments()) {
                                        String appointmentChildChecker = documentData.getString("childvaccine");
                                        String appointmentAdultChecker = documentData.getString("adultvaccine");
                                        String primary_Key = documentData.getString("pkIdentifier");
                                        String location = documentData.getString("location");
                                        String appointmentDocumentID = documentData.getId();
                                        if ("FALSE".equals(selectedchildState.get())) {
                                            System.out.println(appointmentAdultChecker);
                                            if (appointmentAdultChecker == null || appointmentAdultChecker.equals("") || appointmentAdultChecker.equals("None")) {
                                                System.out.println("Dumaan");
                                                Platform.runLater(() -> {
                                                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                                                    alert.setTitle("Invalid Appointment");
                                                    alert.setContentText("It seems that there are no appointments selected.\n Please select an appointment.");
                                                    alert.showAndWait();
                                                });
                                            } else {
                                                try (PDDocument PDFDocument = new PDDocument()) {
                                                    PDPage page = new PDPage(new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()));
                                                    PDFDocument.addPage(page);
                                                    DocumentReference userCollection = db.collection("users").document(gbuserIdentifier);
                                                    ApiFuture<DocumentSnapshot> userQuerySnapshot = userCollection.get();
                                                    try {
                                                        DocumentSnapshot userDocument = userQuerySnapshot.get();
                                                        if (userDocument.exists()) {
                                                            String userFirstName = userDocument.getString("firstName");
                                                            String userLastName = userDocument.getString("lastName");
                                                            String dateofBirth = userDocument.getString("dateofbirth");
                                                            LocalDate currentDate = LocalDate.now();
                                                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d/yyyy");
                                                            String formattedDate = currentDate.format(formatter);
                                                            LocalTime currentTime = LocalTime.now();
                                                            DateTimeFormatter formatterTime = DateTimeFormatter.ofPattern("h:mm a");
                                                            String formattedTime = currentTime.format(formatterTime);
                                                            try (PDPageContentStream contentStream = new PDPageContentStream(PDFDocument, page)) {
                                                                InputStream logoStream = getClass().getResourceAsStream("/assets/logo_removebg_preview__1_.png");
                                                                BufferedImage bufferedImageLogo = ImageIO.read(logoStream);
                                                                BufferedImage transparentImage = new BufferedImage(bufferedImageLogo.getWidth(), bufferedImageLogo.getHeight(), BufferedImage.TYPE_INT_ARGB);
                                                                Graphics2D g2d = transparentImage.createGraphics();
                                                                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f)); // 50% transparency
                                                                g2d.drawImage(bufferedImageLogo, 0, 0, null);
                                                                g2d.dispose();
                                                                PDImageXObject imageLogo = LosslessFactory.createFromImage(PDFDocument, transparentImage);
                                                                contentStream.drawImage(imageLogo, 280, 220, imageLogo.getWidth(), imageLogo.getHeight());

                                                                InputStream borderStream = getClass().getResourceAsStream("/assets/login_page_wireframe_mobile_prototype_removebg_preview__1_.png");
                                                                BufferedImage bufferedImageBorder = ImageIO.read(borderStream);
                                                                PDImageXObject imageBorder = LosslessFactory.createFromImage(PDFDocument, bufferedImageBorder);

                                                                contentStream.drawImage(imageBorder, 0, 372, 330, 230);
                                                                BufferedImage bufferedImage = ImageIO.read(new URL(clientSignature));
                                                                PDImageXObject imageSignature = LosslessFactory.createFromImage(PDFDocument, bufferedImage);
                                                                contentStream.saveGraphicsState();
                                                                contentStream.setNonStrokingColor(1, 1, 1, 0.1f);
                                                                contentStream.drawImage(imageSignature, 580, 96, 180, 100);
                                                                contentStream.restoreGraphicsState();
                                                                drawText(contentStream, PDType1Font.HELVETICA_BOLD, 30, "Vaccination Certificate", 270, 480);
                                                                drawText(contentStream, PDType1Font.HELVETICA_BOLD, 20, "Personal Info: ", 120, 450);
                                                                drawText(contentStream, PDType1Font.HELVETICA_BOLD, 12, "First Name: " + userFirstName, 120, 420);
                                                                drawText(contentStream, PDType1Font.HELVETICA_BOLD, 12, "Last Name: " + userLastName, 120, 390);
                                                                drawText(contentStream, PDType1Font.HELVETICA_BOLD, 12, "Date of Birth: " + dateofBirth, 120, 360);
                                                                drawText(contentStream, PDType1Font.HELVETICA_BOLD, 12, "Location: " + location, 120, 330);
                                                                drawText(contentStream, PDType1Font.HELVETICA_BOLD, 12, "Administered By: " + clientFirstname + " " + clientLastname, 580, 85);
                                                                drawText(contentStream, PDType1Font.HELVETICA_BOLD, 12, clientTitle, 635, 70);
                                                                drawText(contentStream, PDType1Font.HELVETICA_BOLD, 12, clientLicense_Number, 628, 50);
                                                                drawText(contentStream, PDType1Font.HELVETICA_BOLD, 12, "In Case of Side Effects, Please contact Immunicare", 85, 50);
                                                                drawText(contentStream, PDType1Font.HELVETICA_BOLD, 20, "Vaccine Taken : ", 85, 288);

                                                                float margin = 85;
                                                                float yStart = page.getMediaBox().getHeight() - (margin + 250);
                                                                float tableWidth = page.getMediaBox().getWidth() - 2 * margin;
                                                                float yPosition = yStart;
                                                                float tableHeight = 20;
                                                                float rowHeight = tableHeight / 3;
                                                                float tableWidthMargin = page.getMediaBox().getWidth() - 2 * margin;

                                                                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
                                                                contentStream.beginText();
                                                                contentStream.newLineAtOffset(margin, yStart);
                                                                contentStream.showText(" Vaccine");
                                                                contentStream.newLineAtOffset(tableWidthMargin / 4, 0);
                                                                contentStream.showText(" Date");
                                                                contentStream.newLineAtOffset(tableWidthMargin / 4, 0);
                                                                contentStream.showText(" Time");
                                                                contentStream.newLineAtOffset(tableWidthMargin / 4, 0);
                                                                contentStream.showText(" Vaccination Key");
                                                                contentStream.endText();
                                                                contentStream.setLineWidth(1f);
                                                                contentStream.moveTo(margin, yPosition - rowHeight);
                                                                contentStream.lineTo(margin + tableWidth, yPosition - rowHeight);
                                                                contentStream.stroke();
                                                                for (int i = 0; i < 4; i++) {
                                                                    contentStream.moveTo(margin + i * (tableWidth / 4), yPosition);
                                                                    contentStream.lineTo(margin + i * (tableWidth / 4), yPosition - tableHeight);
                                                                    contentStream.stroke();
                                                                }
                                                                String[][] tableContent = {
                                                                    {"" + appointmentAdultChecker, "" + formattedDate, "" + formattedTime, "" + appointmentKeyforList}

                                                                };
                                                                contentStream.setFont(PDType1Font.HELVETICA, 12);
                                                                float textx = margin + 4;
                                                                float texty = yPosition - 22;
                                                                for (String[] row : tableContent) {
                                                                    for (String cell : row) {
                                                                        float cellWidth = tableWidth / 4;
                                                                        List<String> lines = splitText(cell, PDType1Font.HELVETICA, 12, cellWidth);
                                                                        for (String line : lines) {
                                                                            contentStream.beginText();
                                                                            contentStream.newLineAtOffset(textx, texty);
                                                                            contentStream.showText(line);
                                                                            contentStream.endText();
                                                                            texty -= rowHeight;
                                                                        }

                                                                        textx += cellWidth;
                                                                        texty = yPosition - 22;
                                                                    }
                                                                }
                                                            } catch (Exception e1) {
                                                                e1.printStackTrace();
                                                            }
                                                            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                                                            PDFDocument.save(byteArrayOutputStream);
                                                            PDFDocument.close();
                                                            byte[] pdfBytes = byteArrayOutputStream.toByteArray();
                                                            String storagePath = "Certificates/" + primary_Key + "/" + appointmentAdultChecker + ".pdf";
                                                            BlobId blobId = BlobId.of(bucketName, storagePath);
                                                            BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType("application/pdf").build();
                                                            storage.create(blobInfo, pdfBytes);
                                                            String downloadUrl = storage.get(blobId).toBuilder().build().getMediaLink();
                                                            Map<String, Object> updates = new HashMap<>();
                                                            Map<String, Object> vaccine_counter = new HashMap<>();
                                                            updates.put("appointmentKey", appointmentKeyforList);
                                                            updates.put("adultvaccine", appointmentAdultChecker);
                                                            updates.put("childvaccine", "");
                                                            updates.put("status", "Fulfilled");
                                                            updates.put("date", formattedDate);
                                                            updates.put("time", formattedTime);
                                                            updates.put("location", location);
                                                            updates.put("pkIdentifier", primary_Key);
                                                            updates.put("adultCertificateURL", downloadUrl);
                                                            updates.put("childCertificateURL", "");
                                                            vaccine_counter.put("vaccinationDate", formattedDate);
                                                            vaccine_counter.put("vaccinationTime", formattedTime);
                                                            vaccine_counter.put("vaccine", appointmentAdultChecker);
                                                            vaccine_counter.put("appointmentKey", appointmentKeyforList);
                                                            vaccine_counter.put("client_practitioner", clientFirstname + " " + clientLastname);
                                                            vaccine_counter.put("client_user", userFirstName + " " + userLastName);
                                                            vaccine_counter.put("title", clientTitle);

                                                            Map<String, Object> existingMap_1 = (Map<String, Object>) userDocument.get("appointmentList");
                                                            String keyToRemove = findKeyByValue(existingMap_1, appointmentKeyforList);
                                                            existingMap_1.remove(keyToRemove);
                                                            Map<String, Object> modifiedMap = subtractOneFromKeys(existingMap_1, keyToRemove);
                                                            ApiFuture<WriteResult> updateFuture = userCollection.update("appointmentList", modifiedMap);
                                                            updateFuture.get();
                                                            String vaccination_count = userDocument.getString("vaccination_count");
                                                            int vaccination_count_tracker = Integer.parseInt(vaccination_count);
                                                            vaccination_count_tracker--;
                                                            db.collection("users").document(gbuserIdentifier).update("vaccination_count", Integer.toString(vaccination_count_tracker));
                                                            db.collection("users").document(gbuserIdentifier).collection("deletedAppointments").add(updates);
                                                            db.collection("users").document(gbuserIdentifier).collection("adultCompletedAppointments").add(updates);
                                                            db.collection("users").document(gbuserIdentifier).collection("appointments").document(appointmentDocumentID).delete();
                                                            db.collection("adultappointment_data").add(vaccine_counter);
                                                            if (existingMap_1.isEmpty()) {
                                                                db.collection("users").document(gbuserIdentifier).update("vaccination_count", "0");
                                                            }
                                                            initializeTableModelForUser();
                                                            Platform.runLater(() -> {
                                                                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                                                                alert.setTitle("Information");
                                                                alert.setContentText("User Vaccine Confirmed");
                                                                alert.showAndWait();
                                                            });
                                                        }
                                                    } catch (Exception e1) {
                                                        e1.printStackTrace();
                                                    }
                                                } catch (IOException e2) {
                                                    e2.printStackTrace();
                                                }
                                            }
                                        } else {
                                            if (appointmentChildChecker.equals("Others(if not listed)") || (appointmentAdultChecker.equals("None") && appointmentChildChecker.equals("None"))) {
                                                Platform.runLater(() -> {
                                                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                                                    alert.setTitle("Invalid Appointment");
                                                    alert.setContentText("Invalid Vaccine Entry.\n Please select an Update Appointment.");
                                                    alert.showAndWait();
                                                });
                                            } else if ((appointmentChildChecker.equals("") || appointmentChildChecker.equals("None")) && (!appointmentAdultChecker.equals("") || !appointmentAdultChecker.equals("None"))) {
                                                try (PDDocument PDFDocument = new PDDocument()) {
                                                    PDPage page = new PDPage(new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()));
                                                    PDFDocument.addPage(page);
                                                    DocumentReference userCollection = db.collection("users").document(gbuserIdentifier);
                                                    ApiFuture<DocumentSnapshot> userQuerySnapshot = userCollection.get();
                                                    try {
                                                        DocumentSnapshot userDocument = userQuerySnapshot.get();
                                                        if (userDocument.exists()) {
                                                            String userFirstName = userDocument.getString("firstName");
                                                            String userLastName = userDocument.getString("lastName");
                                                            String dateofBirth = userDocument.getString("dateofbirth");
                                                            LocalDate currentDate = LocalDate.now();
                                                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d/yyyy");
                                                            String formattedDate = currentDate.format(formatter);
                                                            LocalTime currentTime = LocalTime.now();
                                                            DateTimeFormatter formatterTime = DateTimeFormatter.ofPattern("h:mm a");
                                                            String formattedTime = currentTime.format(formatterTime);
                                                            try (PDPageContentStream contentStream = new PDPageContentStream(PDFDocument, page)) {

                                                                InputStream logoStream = getClass().getResourceAsStream("/assets/logo_removebg_preview__1_.png");
                                                                BufferedImage bufferedImageLogo = ImageIO.read(logoStream);
                                                                BufferedImage transparentImage = new BufferedImage(bufferedImageLogo.getWidth(), bufferedImageLogo.getHeight(), BufferedImage.TYPE_INT_ARGB);
                                                                Graphics2D g2d = transparentImage.createGraphics();
                                                                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
                                                                g2d.drawImage(bufferedImageLogo, 0, 0, null);
                                                                g2d.dispose();
                                                                PDImageXObject imageLogo = LosslessFactory.createFromImage(PDFDocument, transparentImage);
                                                                contentStream.drawImage(imageLogo, 280, 220, imageLogo.getWidth(), imageLogo.getHeight());

                                                                InputStream borderStream = getClass().getResourceAsStream("/assets/login_page_wireframe_mobile_prototype_removebg_preview__1_.png");
                                                                BufferedImage bufferedImageBorder = ImageIO.read(borderStream);
                                                                PDImageXObject imageBorder = LosslessFactory.createFromImage(PDFDocument, bufferedImageBorder);
                                                                contentStream.drawImage(imageBorder, 0, 372, 330, 230);
                                                                BufferedImage bufferedImage = ImageIO.read(new URL(clientSignature));
                                                                PDImageXObject imageSignature = LosslessFactory.createFromImage(PDFDocument, bufferedImage);
                                                                contentStream.saveGraphicsState();
                                                                contentStream.setNonStrokingColor(1, 1, 1, 0.1f);
                                                                contentStream.drawImage(imageSignature, 580, 96, 180, 100);
                                                                contentStream.restoreGraphicsState();
                                                                drawText(contentStream, PDType1Font.HELVETICA_BOLD, 30, "Vaccination Certificate", 270, 480);
                                                                drawText(contentStream, PDType1Font.HELVETICA_BOLD, 20, "Personal Info: ", 120, 450);
                                                                drawText(contentStream, PDType1Font.HELVETICA_BOLD, 12, "First Name: " + userFirstName, 120, 420);
                                                                drawText(contentStream, PDType1Font.HELVETICA_BOLD, 12, "Last Name: " + userLastName, 120, 390);
                                                                drawText(contentStream, PDType1Font.HELVETICA_BOLD, 12, "Date of Birth: " + dateofBirth, 120, 360);
                                                                drawText(contentStream, PDType1Font.HELVETICA_BOLD, 12, "Location: " + location, 120, 330);
                                                                drawText(contentStream, PDType1Font.HELVETICA_BOLD, 12, "Administered By: " + clientFirstname + " " + clientLastname, 580, 85);
                                                                drawText(contentStream, PDType1Font.HELVETICA_BOLD, 12, clientTitle, 635, 70);
                                                                drawText(contentStream, PDType1Font.HELVETICA_BOLD, 12, clientLicense_Number, 628, 50);
                                                                drawText(contentStream, PDType1Font.HELVETICA_BOLD, 12, "In Case of Side Effects, Please contact Immunicare", 85, 50);
                                                                drawText(contentStream, PDType1Font.HELVETICA_BOLD, 20, "Vaccine Taken : ", 85, 288);

                                                                float margin = 85;
                                                                float yStart = page.getMediaBox().getHeight() - (margin + 250);
                                                                float tableWidth = page.getMediaBox().getWidth() - 2 * margin;
                                                                float yPosition = yStart;
                                                                float tableHeight = 20;
                                                                float rowHeight = tableHeight / 3;
                                                                float tableWidthMargin = page.getMediaBox().getWidth() - 2 * margin;

                                                                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
                                                                contentStream.beginText();
                                                                contentStream.newLineAtOffset(margin, yStart);
                                                                contentStream.showText(" Vaccine");
                                                                contentStream.newLineAtOffset(tableWidthMargin / 4, 0);
                                                                contentStream.showText(" Date");
                                                                contentStream.newLineAtOffset(tableWidthMargin / 4, 0);
                                                                contentStream.showText(" Time");
                                                                contentStream.newLineAtOffset(tableWidthMargin / 4, 0);
                                                                contentStream.showText(" Vaccination Key");
                                                                contentStream.endText();
                                                                contentStream.setLineWidth(1f);
                                                                contentStream.moveTo(margin, yPosition - rowHeight);
                                                                contentStream.lineTo(margin + tableWidth, yPosition - rowHeight);
                                                                contentStream.stroke();
                                                                for (int i = 0; i < 4; i++) {
                                                                    contentStream.moveTo(margin + i * (tableWidth / 4), yPosition);
                                                                    contentStream.lineTo(margin + i * (tableWidth / 4), yPosition - tableHeight);
                                                                    contentStream.stroke();
                                                                }
                                                                String[][] tableContent = {
                                                                    {"" + appointmentAdultChecker, "" + formattedDate, "" + formattedTime, "" + appointmentKeyforList}

                                                                };
                                                                contentStream.setFont(PDType1Font.HELVETICA, 12);
                                                                float textx = margin + 4;
                                                                float texty = yPosition - 22;
                                                                for (String[] row : tableContent) {
                                                                    for (String cell : row) {
                                                                        float cellWidth = tableWidth / 4;
                                                                        List<String> lines = splitText(cell, PDType1Font.HELVETICA, 12, cellWidth);
                                                                        for (String line : lines) {
                                                                            contentStream.beginText();
                                                                            contentStream.newLineAtOffset(textx, texty);
                                                                            contentStream.showText(line);
                                                                            contentStream.endText();
                                                                            texty -= rowHeight;
                                                                        }

                                                                        textx += cellWidth;
                                                                        texty = yPosition - 22;
                                                                    }
                                                                }
                                                            } catch (Exception e1) {
                                                                e1.printStackTrace();
                                                            }
                                                            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                                                            PDFDocument.save(byteArrayOutputStream);
                                                            PDFDocument.close();
                                                            byte[] pdfBytes = byteArrayOutputStream.toByteArray();
                                                    
                                                            String storagePath = "Certificates/" + primary_Key + "/" + appointmentAdultChecker + ".pdf";
                                                            BlobId blobId = BlobId.of(bucketName, storagePath);
                                                            BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType("application/pdf").build();
                                                            storage.create(blobInfo, pdfBytes);
                                                            String downloadUrl = storage.get(blobId).toBuilder().build().getMediaLink();
                                                            Map<String, Object> updates = new HashMap<>();
                                                            Map<String, Object> vaccine_counter = new HashMap<>();
                                                            updates.put("appointmentKey", appointmentKeyforList);
                                                            updates.put("adultvaccine", appointmentAdultChecker);
                                                            updates.put("childvaccine", "");
                                                            updates.put("status", "Fulfilled");
                                                            updates.put("date", formattedDate);
                                                            updates.put("time", formattedTime);
                                                            updates.put("location", location);
                                                            updates.put("pkIdentifier", primary_Key);
                                                            updates.put("adultCertificateURL", downloadUrl);
                                                            updates.put("childCertificateURL", "");
                                                            vaccine_counter.put("vaccinationDate", formattedDate);
                                                            vaccine_counter.put("vaccinationTime", formattedTime);
                                                            vaccine_counter.put("vaccine", appointmentAdultChecker);
                                                            vaccine_counter.put("appointmentKey", appointmentKeyforList);
                                                            vaccine_counter.put("client_practitioner", clientFirstname + " " + clientLastname);
                                                            vaccine_counter.put("client_user", userFirstName + " " + userLastName);
                                                            vaccine_counter.put("title", clientTitle);

                                                            Map<String, Object> existingMap_1 = (Map<String, Object>) userDocument.get("appointmentList");
                                                            String keyToRemove = findKeyByValue(existingMap_1, appointmentKeyforList);
                                                            existingMap_1.remove(keyToRemove);
                                                            Map<String, Object> modifiedMap = subtractOneFromKeys(existingMap_1, keyToRemove);
                                                            ApiFuture<WriteResult> updateFuture = userCollection.update("appointmentList", modifiedMap);
                                                            updateFuture.get();
                                                            String vaccination_count = userDocument.getString("vaccination_count");
                                                            int vaccination_count_tracker = Integer.parseInt(vaccination_count);
                                                            vaccination_count_tracker--;
                                                            db.collection("users").document(gbuserIdentifier).update("vaccination_count", Integer.toString(vaccination_count_tracker));
                                                            db.collection("users").document(gbuserIdentifier).collection("deletedAppointments").add(updates);
                                                            db.collection("users").document(gbuserIdentifier).collection("adultCompletedAppointments").add(updates);
                                                            db.collection("users").document(gbuserIdentifier).collection("appointments").document(appointmentDocumentID).delete();
                                                            db.collection("adultappointment_data").add(vaccine_counter);
                                                            if (existingMap_1.isEmpty()) {
                                                                db.collection("users").document(gbuserIdentifier).update("vaccination_count", "0");
                                                            }
                                                            initializeTableModelForUser();
                                                            Platform.runLater(() -> {
                                                                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                                                                alert.setTitle("Information");
                                                                alert.setContentText("User Vaccine Confirmed");
                                                                alert.showAndWait();
                                                            });
                                                        }
                                                    } catch (Exception e1) {
                                                        e1.printStackTrace();
                                                    }
                                                } catch (IOException e2) {
                                                    e2.printStackTrace();
                                                }
                                            } else if ((appointmentAdultChecker.equals("") || appointmentAdultChecker.equals("None")) && (!appointmentChildChecker.equals("") || !appointmentChildChecker.equals("None"))) {
                                                try (PDDocument PDFDocument = new PDDocument()) {
                                                    String childIdentifier = "";
                                                    PDPage page = new PDPage(new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()));
                                                    PDFDocument.addPage(page);
                                                    DocumentReference userDocument = db.collection("users").document(gbuserIdentifier);
                                                    ApiFuture<DocumentSnapshot> future = userDocument.get();
                                                    try {
                                                        DocumentSnapshot document = future.get();
                                                        if (document.exists()) {
                                                            childIdentifier = document.getString("childIdentifier");
                                                        }
                                                    } catch (Exception e1) {
                                                        e1.printStackTrace();
                                                    }
                                                    CollectionReference childCollection = db.collection("users").document(gbuserIdentifier).collection("child");
                                                    ApiFuture<QuerySnapshot> querySnapshotFuture = childCollection.whereEqualTo("childIdentifier", childIdentifier).get();
                                                    QuerySnapshot querySnapshot;
                                                    try {
                                                        querySnapshot = querySnapshotFuture.get();
                                                        for (QueryDocumentSnapshot childdocument : querySnapshot.getDocuments()) {
                                                            String childFirstName = childdocument.getString("childName");
                                                            String childLastName = childdocument.getString("childLastName");
                                                            String childDOB = childdocument.getString("child_dob");
                                                            LocalDate currentDate = LocalDate.now();
                                                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d/yyyy");
                                                            String formattedDate = currentDate.format(formatter);
                                                            LocalTime currentTime = LocalTime.now();
                                                            DateTimeFormatter formatterTime = DateTimeFormatter.ofPattern("h:mm a");
                                                            String formattedTime = currentTime.format(formatterTime);
                                                            try (PDPageContentStream contentStream = new PDPageContentStream(PDFDocument, page)) {
                                                                InputStream logoStream = getClass().getResourceAsStream("/assets/logo_removebg_preview__1_.png");
                                                                BufferedImage bufferedImageLogo = ImageIO.read(logoStream);
                                                                BufferedImage transparentImage = new BufferedImage(bufferedImageLogo.getWidth(), bufferedImageLogo.getHeight(), BufferedImage.TYPE_INT_ARGB);
                                                                Graphics2D g2d = transparentImage.createGraphics();
                                                                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f)); // 50% transparency
                                                                g2d.drawImage(bufferedImageLogo, 0, 0, null);
                                                                g2d.dispose();
                                                                PDImageXObject imageLogo = LosslessFactory.createFromImage(PDFDocument, transparentImage);
                                                                contentStream.drawImage(imageLogo, 280, 220, imageLogo.getWidth(), imageLogo.getHeight());

                                                                InputStream borderStream = getClass().getResourceAsStream("/assets/login_page_wireframe_mobile_prototype_removebg_preview__1_.png");
                                                                BufferedImage bufferedImageBorder = ImageIO.read(borderStream);
                                                                PDImageXObject imageBorder = LosslessFactory.createFromImage(PDFDocument, bufferedImageBorder);
                                                                contentStream.drawImage(imageBorder, 0, 372, 330, 230);
                                                                BufferedImage bufferedImage = ImageIO.read(new URL(clientSignature));
                                                                PDImageXObject imageSignature = LosslessFactory.createFromImage(PDFDocument, bufferedImage);
                                                                contentStream.saveGraphicsState();
                                                                contentStream.setNonStrokingColor(1, 1, 1, 0.1f);
                                                                contentStream.drawImage(imageSignature, 580, 96, 180, 100);
                                                                contentStream.restoreGraphicsState();
                                                                drawText(contentStream, PDType1Font.HELVETICA_BOLD, 30, "Vaccination Certificate", 270, 480);
                                                                drawText(contentStream, PDType1Font.HELVETICA_BOLD, 20, "Personal Info: ", 120, 450);
                                                                drawText(contentStream, PDType1Font.HELVETICA_BOLD, 12, "First Name: " + childFirstName, 120, 420);
                                                                drawText(contentStream, PDType1Font.HELVETICA_BOLD, 12, "Last Name: " + childLastName, 120, 390);
                                                                drawText(contentStream, PDType1Font.HELVETICA_BOLD, 12, "Date of Birth: " + childDOB, 120, 360);
                                                                drawText(contentStream, PDType1Font.HELVETICA_BOLD, 12, "Location: " + location, 120, 330);
                                                                drawText(contentStream, PDType1Font.HELVETICA_BOLD, 12, "Administered by: " + clientFirstname + " " + clientLastname, 580, 85);
                                                                drawText(contentStream, PDType1Font.HELVETICA_BOLD, 12, clientTitle, 635, 70);
                                                                drawText(contentStream, PDType1Font.HELVETICA_BOLD, 12, clientLicense_Number, 628, 50);
                                                                drawText(contentStream, PDType1Font.HELVETICA_BOLD, 12, "In Case of Side Effects, Please contact Immunicare", 85, 50);
                                                                drawText(contentStream, PDType1Font.HELVETICA_BOLD, 20, "Vaccine Taken : ", 85, 288);

                                                                float margin = 85;
                                                                float yStart = page.getMediaBox().getHeight() - (margin + 250);
                                                                float tableWidth = page.getMediaBox().getWidth() - 2 * margin;
                                                                float yPosition = yStart;
                                                                float tableHeight = 20;
                                                                float rowHeight = tableHeight / 3;
                                                                float tableWidthMargin = page.getMediaBox().getWidth() - 2 * margin;
                                                                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
                                                                contentStream.beginText();
                                                                contentStream.newLineAtOffset(margin, yStart);
                                                                contentStream.showText(" Vaccine");
                                                                contentStream.newLineAtOffset(tableWidthMargin / 4, 0);
                                                                contentStream.showText(" Date");
                                                                contentStream.newLineAtOffset(tableWidthMargin / 4, 0);
                                                                contentStream.showText(" Time");
                                                                contentStream.newLineAtOffset(tableWidthMargin / 4, 0);
                                                                contentStream.showText(" Vaccination Key");
                                                                contentStream.endText();
                                                                contentStream.setLineWidth(1f);
                                                                contentStream.moveTo(margin, yPosition - rowHeight);
                                                                contentStream.lineTo(margin + tableWidth, yPosition - rowHeight);
                                                                contentStream.stroke();
                                                                for (int i = 0; i < 4; i++) {
                                                                    contentStream.moveTo(margin + i * (tableWidth / 4), yPosition);
                                                                    contentStream.lineTo(margin + i * (tableWidth / 4), yPosition - tableHeight);
                                                                    contentStream.stroke();
                                                                }
                                                                String[][] tableContent = {
                                                                    {"" + appointmentChildChecker, "" + formattedDate, "" + formattedTime, "" + appointmentKeyforList}

                                                                };
                                                                contentStream.setFont(PDType1Font.HELVETICA, 12);
                                                                float textx = margin + 4;
                                                                float texty = yPosition - 22;
                                                                for (String[] row : tableContent) {
                                                                    for (String cell : row) {
                                                                        float cellWidth = tableWidth / 4;
                                                                        List<String> lines = splitText(cell, PDType1Font.HELVETICA, 12, cellWidth);
                                                                        for (String line : lines) {
                                                                            contentStream.beginText();
                                                                            contentStream.newLineAtOffset(textx, texty);
                                                                            contentStream.showText(line);
                                                                            contentStream.endText();
                                                                            texty -= rowHeight;
                                                                        }

                                                                        textx += cellWidth;
                                                                        texty = yPosition - 22;
                                                                    }

                                                                }
                                                            } catch (Exception e1) {
                                                                e1.printStackTrace();
                                                            }
                                                            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                                                            PDFDocument.save(byteArrayOutputStream);
                                                            PDFDocument.close();
                                                            byte[] pdfBytes = byteArrayOutputStream.toByteArray();
                                                    
                                                            String storagePath = "Certificates/" + primary_Key + "/child/" + appointmentChildChecker + ".pdf";
                                                            BlobId blobId = BlobId.of(bucketName, storagePath);
                                                            BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType("application/pdf").build();
                                                            storage.create(blobInfo, pdfBytes);
                                                            String downloadUrl = storage.get(blobId).toBuilder().build().getMediaLink();
                                                            Map<String, Object> updates = new HashMap<>();
                                                            Map<String, Object> vaccine_counter = new HashMap<>();
                                                            vaccine_counter.put("vaccinationDate", formattedDate);
                                                            vaccine_counter.put("vaccinationTime", formattedTime);
                                                            vaccine_counter.put("vaccine", appointmentChildChecker);
                                                            vaccine_counter.put("appointmentKey", appointmentKeyforList);
                                                            vaccine_counter.put("client_practitioner", clientFirstname + " " + clientLastname);
                                                            vaccine_counter.put("client_user", childFirstName + " " + childLastName);
                                                            vaccine_counter.put("title", clientTitle);
                                                            updates.put("appointmentKey", appointmentKeyforList);
                                                            updates.put("adultvaccine", "");
                                                            updates.put("childvaccine", appointmentChildChecker);
                                                            updates.put("status", "Fulfilled");
                                                            updates.put("date", formattedDate);
                                                            updates.put("time", formattedTime);
                                                            updates.put("location", location);
                                                            updates.put("pkIdentifier", primary_Key);
                                                            updates.put("adultCertificateURL", "");
                                                            updates.put("childCertificateURL", downloadUrl);
                                                            DocumentReference userCollection = db.collection("users").document(gbuserIdentifier);
                                                            ApiFuture<DocumentSnapshot> userQuerySnapshot = userCollection.get();
                                                            DocumentSnapshot userDocumentData;
                                                            try {
                                                                userDocumentData = userQuerySnapshot.get();
                                                                if (userDocumentData.exists()) {
                                                                    Map<String, Object> existingMap_1 = (Map<String, Object>) userDocumentData.get("appointmentList");
                                                                    String keyToRemove = findKeyByValue(existingMap_1, appointmentKeyforList);
                                                                    existingMap_1.remove(keyToRemove);
                                                                    Map<String, Object> modifiedMap = subtractOneFromKeys(existingMap_1, keyToRemove);
                                                                    ApiFuture<WriteResult> updateFuture = userCollection.update("appointmentList", modifiedMap);
                                                                    updateFuture.get();
                                                                    String vaccination_count = userDocumentData.getString("vaccination_count");
                                                                    int vaccination_count_tracker = Integer.parseInt(vaccination_count);
                                                                    vaccination_count_tracker--;
                                                                    db.collection("users").document(gbuserIdentifier).update("vaccination_count", Integer.toString(vaccination_count_tracker));
                                                                    db.collection("users").document(gbuserIdentifier).collection("deletedAppointments").add(updates);
                                                                    db.collection("users").document(gbuserIdentifier).collection("childCompletedAppointments").add(updates);
                                                                    db.collection("users").document(gbuserIdentifier).collection("appointments").document(appointmentDocumentID).delete();
                                                                    db.collection("childappointment_data").add(vaccine_counter);
                                                                    if (existingMap_1.isEmpty()) {
                                                                        db.collection("users").document(gbuserIdentifier).update("vaccination_count", "0");
                                                                    }
                                                                    initializeTableModelForUser();
                                                                    Platform.runLater(() -> {
                                                                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                                                                        alert.setTitle("Information");
                                                                        alert.setContentText("User Vaccine Confirmed");
                                                                        alert.showAndWait();
                                                                    });
                                                                }
                                                            } catch (InterruptedException | ExecutionException e) {
                                                                e.printStackTrace();
                                                            }

                                                        }

                                                    } catch (InterruptedException | ExecutionException e) {
                                                        e.printStackTrace();
                                                    }

                                                } catch (IOException e3) {
                                                    e3.printStackTrace();
                                                }
                                            } else if ((!appointmentAdultChecker.equals("") || !appointmentAdultChecker.equals("None")) && (!appointmentChildChecker.equals("") || !appointmentChildChecker.equals("None"))) {
                                                String childCertificateURL = null, adultCertificateURL = null;
                                                String childIdentifier = "";
                                                Map<String, Object> childupdates = new HashMap<>();
                                                Map<String, Object> adultupdates = new HashMap<>();
                                                Map<String, Object> updates = new HashMap<>();
                                                Map<String, Object> adultvaccine_counter = new HashMap<>();
                                                Map<String, Object> childvaccine_counter = new HashMap<>();
                                                LocalDate currentDate = LocalDate.now();
                                                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d/yyyy");
                                                String formattedDate = currentDate.format(formatter);
                                                LocalTime currentTime = LocalTime.now();
                                                DateTimeFormatter formatterTime = DateTimeFormatter.ofPattern("h:mm a");
                                                String formattedTime = currentTime.format(formatterTime);
                                                try {
                                                    try (PDDocument adultPDFDocument = new PDDocument()) {
                                                        PDPage page = new PDPage(new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()));
                                                        adultPDFDocument.addPage(page);
                                                        DocumentReference userCollection = db.collection("users").document(gbuserIdentifier);
                                                        ApiFuture<DocumentSnapshot> userQuerySnapshot = userCollection.get();
                                                        try {
                                                            DocumentSnapshot userDocument = userQuerySnapshot.get();
                                                            if (userDocument.exists()) {
                                                                String userFirstName = userDocument.getString("firstName");
                                                                String userLastName = userDocument.getString("lastName");
                                                                String dateofBirth = userDocument.getString("dateofbirth");
                                                                childIdentifier = userDocument.getString("childIdentifier");

                                                                try (PDPageContentStream contentStream = new PDPageContentStream(adultPDFDocument, page)) {
                                                                    InputStream logoStream = getClass().getResourceAsStream("/assets/logo_removebg_preview__1_.png");
                                                                    BufferedImage bufferedImageLogo = ImageIO.read(logoStream);
                                                                    BufferedImage transparentImage = new BufferedImage(bufferedImageLogo.getWidth(), bufferedImageLogo.getHeight(), BufferedImage.TYPE_INT_ARGB);
                                                                    Graphics2D g2d = transparentImage.createGraphics();
                                                                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f)); // 50% transparency
                                                                    g2d.drawImage(bufferedImageLogo, 0, 0, null);
                                                                    g2d.dispose();
                                                                    PDImageXObject imageLogo = LosslessFactory.createFromImage(adultPDFDocument, transparentImage);
                                                                    contentStream.drawImage(imageLogo, 280, 220, imageLogo.getWidth(), imageLogo.getHeight());

                                                                    InputStream borderStream = getClass().getResourceAsStream("/assets/login_page_wireframe_mobile_prototype_removebg_preview__1_.png");
                                                                    BufferedImage bufferedImageBorder = ImageIO.read(borderStream);
                                                                    PDImageXObject imageBorder = LosslessFactory.createFromImage(adultPDFDocument, bufferedImageBorder);
                                                                    contentStream.drawImage(imageBorder, 0, 372, 330, 230);
                                                                    BufferedImage bufferedImage = ImageIO.read(new URL(clientSignature));
                                                                    PDImageXObject imageSignature = LosslessFactory.createFromImage(adultPDFDocument, bufferedImage);
                                                                    contentStream.saveGraphicsState();
                                                                    contentStream.setNonStrokingColor(1, 1, 1, 0.1f);
                                                                    contentStream.drawImage(imageSignature, 580, 96, 180, 100);
                                                                    contentStream.restoreGraphicsState();
                                                                    drawText(contentStream, PDType1Font.HELVETICA_BOLD, 30, "Vaccination Certificate", 270, 480);
                                                                    drawText(contentStream, PDType1Font.HELVETICA_BOLD, 20, "Personal Info: ", 120, 450);
                                                                    drawText(contentStream, PDType1Font.HELVETICA_BOLD, 12, "First Name: " + userFirstName, 120, 420);
                                                                    drawText(contentStream, PDType1Font.HELVETICA_BOLD, 12, "Last Name: " + userLastName, 120, 390);
                                                                    drawText(contentStream, PDType1Font.HELVETICA_BOLD, 12, "Date of Birth: " + dateofBirth, 120, 360);
                                                                    drawText(contentStream, PDType1Font.HELVETICA_BOLD, 12, "Location: " + location, 120, 330);
                                                                    drawText(contentStream, PDType1Font.HELVETICA_BOLD, 12, "Administered By: " + clientFirstname + " " + clientLastname, 580, 85);
                                                                    drawText(contentStream, PDType1Font.HELVETICA_BOLD, 12, clientTitle, 635, 70);
                                                                    drawText(contentStream, PDType1Font.HELVETICA_BOLD, 12, clientLicense_Number, 628, 50);
                                                                    drawText(contentStream, PDType1Font.HELVETICA_BOLD, 12, "In Case of Side Effects, Please contact Immunicare", 85, 50);
                                                                    drawText(contentStream, PDType1Font.HELVETICA_BOLD, 20, "Vaccine Taken : ", 85, 288);

                                                                    float margin = 85;
                                                                    float yStart = page.getMediaBox().getHeight() - (margin + 250);
                                                                    float tableWidth = page.getMediaBox().getWidth() - 2 * margin;
                                                                    float yPosition = yStart;
                                                                    float tableHeight = 20;
                                                                    float rowHeight = tableHeight / 3;
                                                                    float tableWidthMargin = page.getMediaBox().getWidth() - 2 * margin;

                                                                    contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
                                                                    contentStream.beginText();
                                                                    contentStream.newLineAtOffset(margin, yStart);
                                                                    contentStream.showText(" Vaccine");
                                                                    contentStream.newLineAtOffset(tableWidthMargin / 4, 0);
                                                                    contentStream.showText(" Date");
                                                                    contentStream.newLineAtOffset(tableWidthMargin / 4, 0);
                                                                    contentStream.showText(" Time");
                                                                    contentStream.newLineAtOffset(tableWidthMargin / 4, 0);
                                                                    contentStream.showText(" Vaccination Key");
                                                                    contentStream.endText();
                                                                    contentStream.setLineWidth(1f);
                                                                    contentStream.moveTo(margin, yPosition - rowHeight);
                                                                    contentStream.lineTo(margin + tableWidth, yPosition - rowHeight);
                                                                    contentStream.stroke();
                                                                    for (int i = 0; i < 4; i++) {
                                                                        contentStream.moveTo(margin + i * (tableWidth / 4), yPosition);
                                                                        contentStream.lineTo(margin + i * (tableWidth / 4), yPosition - tableHeight);
                                                                        contentStream.stroke();
                                                                    }
                                                                    String[][] tableContent = {
                                                                        {"" + appointmentAdultChecker, "" + formattedDate, "" + formattedTime, "" + appointmentKeyforList}

                                                                    };
                                                                    contentStream.setFont(PDType1Font.HELVETICA, 12);
                                                                    float textx = margin + 4;
                                                                    float texty = yPosition - 22;
                                                                    for (String[] row : tableContent) {
                                                                        for (String cell : row) {
                                                                            float cellWidth = tableWidth / 4;
                                                                            List<String> lines = splitText(cell, PDType1Font.HELVETICA, 12, cellWidth);
                                                                            for (String line : lines) {
                                                                                contentStream.beginText();
                                                                                contentStream.newLineAtOffset(textx, texty);
                                                                                contentStream.showText(line);
                                                                                contentStream.endText();
                                                                                texty -= rowHeight;
                                                                            }

                                                                            textx += cellWidth;
                                                                            texty = yPosition - 22;
                                                                        }
                                                                    }
                                                                } catch (Exception e1) {
                                                                    e1.printStackTrace();
                                                                }
                                                                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                                                                adultPDFDocument.save(byteArrayOutputStream);
                                                                adultPDFDocument.close();
                                                                byte[] pdfBytes = byteArrayOutputStream.toByteArray();
                                                        
                                                                String storagePath = "Certificates/" + primary_Key + "/" + appointmentAdultChecker + ".pdf";
                                                                BlobId blobId = BlobId.of(bucketName, storagePath);
                                                                BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType("application/pdf").build();
                                                                storage.create(blobInfo, pdfBytes);
                                                                adultCertificateURL = storage.get(blobId).toBuilder().build().getMediaLink();
                                                                adultvaccine_counter.put("client_user", userFirstName + " " + userLastName);
                                                            }
                                                        } catch (Exception e3) {
                                                            e3.printStackTrace();

                                                        }

                                                    } catch (IOException e3) {
                                                        e3.printStackTrace();
                                                    }
                                                    try (PDDocument childPDFDocument = new PDDocument()) {
                                                        PDPage page = new PDPage(new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()));
                                                        childPDFDocument.addPage(page);
                                                        CollectionReference childCollection = db.collection("users").document(gbuserIdentifier).collection("child");
                                                        ApiFuture<QuerySnapshot> querySnapshotFuture = childCollection.whereEqualTo("childIdentifier", childIdentifier).get();
                                                        QuerySnapshot querySnapshot = querySnapshotFuture.get();
                                                        for (QueryDocumentSnapshot childdocument : querySnapshot.getDocuments()) {
                                                            String childFirstName = childdocument.getString("childName");
                                                            String childLastName = childdocument.getString("childLastName");
                                                            String childDOB = childdocument.getString("child_dob");
                                                            try (PDPageContentStream contentStream = new PDPageContentStream(childPDFDocument, page)) {
                                                                InputStream logoStream = getClass().getResourceAsStream("/assets/logo_removebg_preview__1_.png");
                                                                BufferedImage bufferedImageLogo = ImageIO.read(logoStream);
                                                                BufferedImage transparentImage = new BufferedImage(bufferedImageLogo.getWidth(), bufferedImageLogo.getHeight(), BufferedImage.TYPE_INT_ARGB);
                                                                Graphics2D g2d = transparentImage.createGraphics();
                                                                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f)); // 50% transparency
                                                                g2d.drawImage(bufferedImageLogo, 0, 0, null);
                                                                g2d.dispose();
                                                                PDImageXObject imageLogo = LosslessFactory.createFromImage(childPDFDocument, transparentImage);
                                                                contentStream.drawImage(imageLogo, 280, 220, imageLogo.getWidth(), imageLogo.getHeight());
                                                                InputStream borderStream = getClass().getResourceAsStream("/assets/login_page_wireframe_mobile_prototype_removebg_preview__1_.png");
                                                                BufferedImage bufferedImageBorder = ImageIO.read(borderStream);
                                                                PDImageXObject imageBorder = LosslessFactory.createFromImage(childPDFDocument, bufferedImageBorder);
                                                                contentStream.drawImage(imageLogo, 280, 220, imageLogo.getWidth(), imageLogo.getHeight());
                                                                contentStream.drawImage(imageBorder, 0, 372, 330, 230);
                                                                BufferedImage bufferedImage = ImageIO.read(new URL(clientSignature));
                                                                PDImageXObject imageSignature = LosslessFactory.createFromImage(childPDFDocument, bufferedImage);
                                                                contentStream.saveGraphicsState();
                                                                contentStream.setNonStrokingColor(1, 1, 1, 0.1f);
                                                                contentStream.drawImage(imageSignature, 580, 96, 180, 100);
                                                                contentStream.restoreGraphicsState();
                                                                drawText(contentStream, PDType1Font.HELVETICA_BOLD, 30, "Vaccination Certificate", 270, 480);
                                                                drawText(contentStream, PDType1Font.HELVETICA_BOLD, 20, "Personal Info: ", 120, 450);
                                                                drawText(contentStream, PDType1Font.HELVETICA_BOLD, 12, "First Name: " + childFirstName, 120, 420);
                                                                drawText(contentStream, PDType1Font.HELVETICA_BOLD, 12, "Last Name: " + childLastName, 120, 390);
                                                                drawText(contentStream, PDType1Font.HELVETICA_BOLD, 12, "Date of Birth: " + childDOB, 120, 360);
                                                                drawText(contentStream, PDType1Font.HELVETICA_BOLD, 12, "Location: " + location, 120, 330);
                                                                drawText(contentStream, PDType1Font.HELVETICA_BOLD, 12, "Administered By: " + clientFirstname + " " + clientLastname, 580, 85);
                                                                drawText(contentStream, PDType1Font.HELVETICA_BOLD, 12, clientTitle, 635, 70);
                                                                drawText(contentStream, PDType1Font.HELVETICA_BOLD, 12, clientLicense_Number, 628, 50);
                                                                drawText(contentStream, PDType1Font.HELVETICA_BOLD, 12, "In Case of Side Effects, Please contact Immunicare", 85, 50);
                                                                drawText(contentStream, PDType1Font.HELVETICA_BOLD, 20, "Vaccine Taken : ", 85, 288);

                                                                float margin = 85;
                                                                float yStart = page.getMediaBox().getHeight() - (margin + 250);
                                                                float tableWidth = page.getMediaBox().getWidth() - 2 * margin;
                                                                float yPosition = yStart;
                                                                float tableHeight = 20;
                                                                float rowHeight = tableHeight / 3;
                                                                float tableWidthMargin = page.getMediaBox().getWidth() - 2 * margin;
                                                                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
                                                                contentStream.beginText();
                                                                contentStream.newLineAtOffset(margin, yStart);
                                                                contentStream.showText(" Vaccine");
                                                                contentStream.newLineAtOffset(tableWidthMargin / 4, 0);
                                                                contentStream.showText(" Date");
                                                                contentStream.newLineAtOffset(tableWidthMargin / 4, 0);
                                                                contentStream.showText(" Time");
                                                                contentStream.newLineAtOffset(tableWidthMargin / 4, 0);
                                                                contentStream.showText(" Vaccination Key");
                                                                contentStream.endText();
                                                                contentStream.setLineWidth(1f);
                                                                contentStream.moveTo(margin, yPosition - rowHeight);
                                                                contentStream.lineTo(margin + tableWidth, yPosition - rowHeight);
                                                                contentStream.stroke();
                                                                for (int i = 0; i < 4; i++) {
                                                                    contentStream.moveTo(margin + i * (tableWidth / 4), yPosition);
                                                                    contentStream.lineTo(margin + i * (tableWidth / 4), yPosition - tableHeight);
                                                                    contentStream.stroke();
                                                                }
                                                                String[][] tableContent = {
                                                                    {"" + appointmentChildChecker, "" + formattedDate, "" + formattedTime, "" + appointmentKeyforList}

                                                                };
                                                                contentStream.setFont(PDType1Font.HELVETICA, 12);
                                                                float textx = margin + 4;
                                                                float texty = yPosition - 22;
                                                                for (String[] row : tableContent) {
                                                                    for (String cell : row) {
                                                                        float cellWidth = tableWidth / 4;
                                                                        List<String> lines = splitText(cell, PDType1Font.HELVETICA, 12, cellWidth);
                                                                        for (String line : lines) {
                                                                            contentStream.beginText();
                                                                            contentStream.newLineAtOffset(textx, texty);
                                                                            contentStream.showText(line);
                                                                            contentStream.endText();
                                                                            texty -= rowHeight;
                                                                        }

                                                                        textx += cellWidth;
                                                                        texty = yPosition - 22;
                                                                    }
                                                                }

                                                            } catch (Exception e1) {
                                                                e1.printStackTrace();
                                                            }
                                                            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                                                            childPDFDocument.save(byteArrayOutputStream);

                                                            byte[] pdfBytes = byteArrayOutputStream.toByteArray();
                                                
                                                            String storagePath = "Certificates/" + primary_Key + "/child/" + appointmentChildChecker + ".pdf";
                                                            BlobId blobId = BlobId.of(bucketName, storagePath);
                                                            BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType("application/pdf").build();
                                                            storage.create(blobInfo, pdfBytes);
                                                            childCertificateURL = storage.get(blobId).toBuilder().build().getMediaLink();
                                                            childvaccine_counter.put("client_user", childFirstName + " " + childLastName);
                                                        }
                                                    } catch (IOException e3) {
                                                        e3.printStackTrace();
                                                    }
                                                    childupdates.put("appointmentKey", appointmentKeyforList);
                                                    childupdates.put("childCertificateURL", childCertificateURL);
                                                    childupdates.put("status", "Fulfilled");
                                                    childupdates.put("date", formattedDate);
                                                    childupdates.put("time", formattedTime);
                                                    childupdates.put("location", location);
                                                    childupdates.put("pkIdentifier", primary_Key);
                                                    childupdates.put("childvaccine", appointmentChildChecker);
                                                    childupdates.put("adultvaccine", "");
                                                    childupdates.put("adultCertificateURL", "");

                                                    adultupdates.put("appointmentKey", appointmentKeyforList);
                                                    adultupdates.put("childCertificateURL", "");
                                                    adultupdates.put("status", "Fulfilled");
                                                    adultupdates.put("date", formattedDate);
                                                    adultupdates.put("time", formattedTime);
                                                    adultupdates.put("location", location);
                                                    adultupdates.put("pkIdentifier", primary_Key);
                                                    adultupdates.put("childvaccine", "");
                                                    adultupdates.put("adultvaccine", appointmentAdultChecker);
                                                    adultupdates.put("adultCertificateURL", adultCertificateURL);
                                                    updates.put("appointmentKey", appointmentKeyforList);
                                                    updates.put("adultvaccine", appointmentAdultChecker);
                                                    updates.put("childvaccine", appointmentChildChecker);
                                                    updates.put("status", "Fulfilled");
                                                    updates.put("date", formattedDate);
                                                    updates.put("time", formattedTime);
                                                    updates.put("location", location);
                                                    updates.put("pkIdentifier", primary_Key);
                                                    updates.put("adultCertificateURL", adultCertificateURL);
                                                    updates.put("childCertificateURL", childCertificateURL);

                                                    adultvaccine_counter.put("vaccinationDate", formattedDate);
                                                    adultvaccine_counter.put("vaccinationTime", formattedTime);
                                                    adultvaccine_counter.put("vaccine", appointmentAdultChecker);
                                                    adultvaccine_counter.put("appointmentKey", appointmentKeyforList);
                                                    adultvaccine_counter.put("client_practitioner", clientFirstname + " " + clientLastname);
                                                    adultvaccine_counter.put("title", clientTitle);

                                                    childvaccine_counter.put("vaccinationDate", formattedDate);
                                                    childvaccine_counter.put("vaccinationTime", formattedTime);
                                                    childvaccine_counter.put("vaccine", appointmentChildChecker);
                                                    childvaccine_counter.put("appointmentKey", appointmentKeyforList);
                                                    childvaccine_counter.put("client_practitioner", clientFirstname + " " + clientLastname);
                                                    childvaccine_counter.put("title", clientTitle);

                                                    DocumentReference userCollection = db.collection("users").document(gbuserIdentifier);
                                                    ApiFuture<DocumentSnapshot> userQuerySnapshot = userCollection.get();
                                                    DocumentSnapshot userDocument = userQuerySnapshot.get();
                                                    if (userDocument.exists()) {
                                                        Map<String, Object> existingMap_1 = (Map<String, Object>) userDocument.get("appointmentList");
                                                        String keyToRemove = findKeyByValue(existingMap_1, appointmentKeyforList);
                                                        existingMap_1.remove(keyToRemove);
                                                        Map<String, Object> modifiedMap = subtractOneFromKeys(existingMap_1, keyToRemove);
                                                        ApiFuture<WriteResult> updateFuture = userCollection.update("appointmentList", modifiedMap);
                                                        updateFuture.get();
                                                        String vaccination_count = userDocument.getString("vaccination_count");
                                                        int vaccination_count_tracker = Integer.parseInt(vaccination_count);
                                                        vaccination_count_tracker--;
                                                        db.collection("users").document(gbuserIdentifier).update("vaccination_count", Integer.toString(vaccination_count_tracker));
                                                        db.collection("users").document(gbuserIdentifier).collection("deletedAppointments").add(updates);
                                                        db.collection("users").document(gbuserIdentifier).collection("adultCompletedAppointments").add(adultupdates);
                                                        db.collection("users").document(gbuserIdentifier).collection("childCompletedAppointments").add(childupdates);
                                                        db.collection("users").document(gbuserIdentifier).collection("appointments").document(appointmentDocumentID).delete();
                                                        db.collection("adultappointment_data").add(adultvaccine_counter);
                                                        db.collection("childappointment_data").add(childvaccine_counter);
                                                        if (existingMap_1.isEmpty()) {
                                                            db.collection("users").document(gbuserIdentifier).update("vaccination_count", "0");
                                                        }
                                                        initializeTableModelForUser();
                                                        Platform.runLater(() -> {
                                                            Alert alert = new Alert(Alert.AlertType.INFORMATION);
                                                            alert.setTitle("Information");
                                                            alert.setContentText("User Vaccine Confirmed");
                                                            alert.showAndWait();
                                                        });

                                                    }

                                                } catch (Exception e4) {
                                                    e4.printStackTrace();
                                                }

                                            } else {
                                                Alert alert = new Alert(Alert.AlertType.ERROR);
                                                alert.setTitle("Error");
                                                alert.setContentText("Cannot Validate Vaccine");
                                                alert.showAndWait();

                                            }

                                        }

                                    }
                                } else {
                                    System.out.println("No documents found.");
                                }
                            });
                        });
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }

                }
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setContentText("Vaccination Confirmation Cancelled");
                alert.showAndWait();
            }

        }
    }

    @FXML
    public void logout(ActionEvent event) {
        if (adapter.getApp() != null) {
            adapter.getApp().delete();
        }
        JavaSession.getInstance().endSession();
        FXMLView nextView = FXMLView.LOGIN;
        stageManager.switchScene(nextView);

    }

    @FXML
    public void refresh(ActionEvent event) {
        initializeTableModelForUser();
        FirstName.clear();
        LastName.clear();
        Age.clear();
        PhoneNumber.clear();
        textSearch.clear();
        switchClickedChecker = false;
        gbuserIdentifier = "";
        userComboBox.getSelectionModel().clearSelection();
        appointmentsView.getSelectionModel().clearSelection();
        vaccineTakenTable.getSelectionModel().clearSelection();
        appointmentsView.getItems().clear();
        vaccineTakenTable.getItems().clear();
        labelImagePane.setText("No Image");
        labelImagePane.setGraphic(null);
    }

    public boolean hasSelectedItem() {
        return appointmentsView.getSelectionModel().getSelectedItem() != null;
    }

    public boolean hasSelectedIteminTable() {
        return userTable.getSelectionModel().getSelectedItem() != null;
    }
}
