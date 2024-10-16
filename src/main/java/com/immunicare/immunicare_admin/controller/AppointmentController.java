package com.immunicare.immunicare_admin.controller;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import org.springframework.stereotype.Controller;

import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.immunicare.immunicare_admin.database.adapter;
import com.immunicare.immunicare_admin.params.SelectedAppointmentData;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.stage.Stage;

@Controller
public class AppointmentController implements Initializable {

    @FXML
    private ComboBox<String> childVaccineCombobox;
    @FXML
    private ComboBox<String> adultVaccineCombobox;
    @FXML
    private Label childVaccineLabel;

    private Firestore db;
    public String initialAppointment, initialChildAppointment, childChecker, initialPrimaryKey, initialAppointmentID;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        ObservableList<String> childVaccineItems = FXCollections.observableArrayList("", "None", "BCG Vaccine", "Hepatitis B", "Tetanus Diptheria (1st Dose)", "Tetanus Diptheria (2nd Dose)", "Tetanus Diptheria (3rd Dose)", "Tetanus Diptheria (4th Dose)", "Tetanus Diptheria (5th Dose)", "Oral Polio Vaccine (1st Dose)", "Oral Polio Vaccine (2nd Dose)", "Oral Polio Vaccine (3rd Dose)", "PENTA Vaccine (DPT-HepB+Hib) (1st Dose)", "PENTA Vaccine (DPT-HepB+Hib) (2nd Dose)", "PENTA Vaccine (DPT-HepB+Hib) (3rd Dose)", "PCV (1st Dose)", "PCV (2nd Dose)", "PCV (3rd Dose)", "Rotavirus Vaccine (1st Dose)", "Rotavirus Vaccine (2nd Dose)", "Inactivated Polio Vaccine", "MMR Vaccine (1st Dose)", "MMR Vaccine (2nd Dose)", "Japanese Encephalitis", "Measles-Rubella Vaccine (1st Dose)", "Measles-Rubella Vaccine (2nd Dose)", "Human Papilloma Virus (HPV) Vaccine", "COVID Vaccine (First Dose)", "COVID Vaccine (Second Dose)", "COVID Vaccine (Booster)");
        childVaccineCombobox.setItems(childVaccineItems);
        ObservableList<String> adultVaccineItems = FXCollections.observableArrayList("", "None", "Pneumo-Polysaccharide Vaccine", "Influenza Vaccine", "COVID Vaccine(First Dose)", "COVID Vaccine(Second Dose)", "COVID Vaccine(Booster)");
        adultVaccineCombobox.setItems(adultVaccineItems);

        initData();
    }

    private void initData() {
        db = adapter.getFirestore();
    }

    @FXML
    public void Update(ActionEvent event) {
        final String childVaccineValue = (String) childVaccineCombobox.getSelectionModel().getSelectedItem();
        final String adultVaccineValue = (String) adultVaccineCombobox.getSelectionModel().getSelectedItem();
        Map<String, Object> updates = new HashMap<>();
        try {
            if (adultVaccineValue.equals(initialAppointment) && childVaccineValue.equals(initialChildAppointment)) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("No Changes Made");
                alert.setContentText("No Changes were Made");
                alert.showAndWait();
            } else {
                if (childChecker != null && !childChecker.equals("No Child")) {
                    updates.clear();
                    updates.put("adultvaccine", adultVaccineValue);
                    updates.put("childvaccine", childVaccineValue);
                    DocumentReference documentReference = db.collection("users").document(initialPrimaryKey).collection("appointments").document(initialAppointmentID);
                    documentReference.update(updates);
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Document Updated");
                    alert.setContentText("Document successfully updated");
                    alert.showAndWait();
                } else {
                    updates.clear();
                    updates.put("adultvaccine", adultVaccineValue);
                    DocumentReference documentReference = db.collection("users").document(initialPrimaryKey).collection("appointments").document(initialAppointmentID);
                    documentReference.update(updates);
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Document Updated");
                    alert.setContentText("Document successfully updated");
                    alert.showAndWait();
                }
            }
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    public void receiveData(SelectedAppointmentData selectedAppointmentData) {
        String primaryKey = selectedAppointmentData.getPrimaryKey();
        String appointmentID = selectedAppointmentData.getAppointmentID();
        String childIdentifier = selectedAppointmentData.getChildIdentifier();
        {
            try {
                if (childIdentifier.equals("No Child")) {
                    childVaccineLabel.setVisible(false);
                    childVaccineCombobox.setVisible(false);

                } else {
                    childVaccineLabel.setVisible(true);
                    childVaccineCombobox.setVisible(true);
                }
                CollectionReference appointmentCollection = db.collection("users").document(primaryKey).collection("appointments");
                DocumentReference documentReference = appointmentCollection.document(appointmentID);
                DocumentSnapshot documentSnapshot = documentReference.get().get();
                if (documentSnapshot.exists()) {
                    String childVaccine = documentSnapshot.getString("childvaccine");
                    String adultVaccine = documentSnapshot.getString("adultvaccine");
                    adultVaccineCombobox.setValue(adultVaccine);
                    childVaccineCombobox.setValue(childVaccine);
                    initialChildAppointment = childVaccine;
                    initialAppointment = adultVaccine;
                    childChecker = childIdentifier;
                    initialPrimaryKey = primaryKey;
                    initialAppointmentID = appointmentID;
                    System.out.println("Child" + childChecker);
                    System.out.println("Appointment ID" + initialAppointmentID);
                    System.out.println("primaryKey" + initialPrimaryKey);

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    @FXML
    private void Back(ActionEvent event) {
        ((Stage) (((Node) event.getSource()).getScene().getWindow())).close();
    }
}
