package com.immunicare.immunicare_admin.controller;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Controller;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.immunicare.immunicare_admin.database.adapter;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.DatePicker;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

@Controller
public class GenerateReportController implements Initializable {

    private Map<String, Integer> childVaccineCounts = new HashMap<>();
    private Map<String, Integer> adultVaccineCounts = new HashMap<>();

    @FXML
    private DatePicker startDate, endDate;

    private Firestore db;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initData();
    }

    private void initData() {
        db = adapter.getFirestore();
    }

    @FXML
    private void Generate(ActionEvent event) {
        LocalDate start = startDate.getValue();
        LocalDate end = endDate.getValue();

        if (start == null || end == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Please ensure to fill up Required Fields");
            alert.showAndWait();
            return;
        }
        if (end.isBefore(start)) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Invalid Date");
            alert.setHeaderText("Please ensure to fill up Valid Date");
            alert.showAndWait();
            return;
        }

        // DateTimeFormatter for m/d/yyyy format
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d/yyyy");

        // Firestore Collections
        CollectionReference childVaccineCollection = db.collection("childappointment_data");
        CollectionReference adultVaccineCollection = db.collection("adultappointment_data");

        try {
            ApiFuture<QuerySnapshot> childFuture = childVaccineCollection.get();
            List<QueryDocumentSnapshot> childDocuments = childFuture.get().getDocuments();

            ApiFuture<QuerySnapshot> adultFuture = adultVaccineCollection.get();
            List<QueryDocumentSnapshot> adultDocuments = adultFuture.get().getDocuments();
            if (childDocuments.isEmpty() && adultDocuments.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("No Data");
                alert.setHeaderText("No Data is Generated");
                alert.showAndWait();
                return;
            }

            boolean foundChildDocuments = false;
            boolean foundAdultDocuments = false;

            try (PDDocument pdfDocument = new PDDocument()) {
                PDPage childPage = new PDPage();
                pdfDocument.addPage(childPage);
                try (PDPageContentStream contentStream = new PDPageContentStream(pdfDocument, childPage)) {
                    contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
                    contentStream.beginText();
                    contentStream.newLineAtOffset(20, 750);
                    contentStream.showText("Appointments Report - Child");
                    contentStream.endText();
                    contentStream.setFont(PDType1Font.HELVETICA, 10);
                    foundChildDocuments = addTable(contentStream, childDocuments, formatter, start, end, pdfDocument, childVaccineCounts);
                }
                // Add Adult Page
                if (foundChildDocuments || !adultDocuments.isEmpty()) {
                    PDPage adultPage = new PDPage();
                    pdfDocument.addPage(adultPage);
                    try (PDPageContentStream contentStream = new PDPageContentStream(pdfDocument, adultPage)) {
                        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
                        contentStream.beginText();
                        contentStream.newLineAtOffset(20, 750);
                        contentStream.showText("Appointments Report - Adult");
                        contentStream.endText();
                        contentStream.setFont(PDType1Font.HELVETICA, 10);
                        foundAdultDocuments = addTable(contentStream, adultDocuments, formatter, start, end, pdfDocument, adultVaccineCounts);
                    }
                }
                // Add Summary and Confidentiality Pages if needed
                if (foundChildDocuments || foundAdultDocuments) {
                    createSummaryPage(pdfDocument, childVaccineCounts, adultVaccineCounts);
                    addConfidentialityStatement(pdfDocument);
                }
                // Save PDF to file
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Save PDF");
                fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
                File file = fileChooser.showSaveDialog((Stage) (((Node) event.getSource()).getScene().getWindow()));
                if (file != null) {
                    pdfDocument.save(file);
                    System.out.println("PDF generated successfully at " + file.getAbsolutePath());
                }
            }

            // Show alert if no documents were found
            if (!foundChildDocuments && !foundAdultDocuments) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("No Results");
                alert.setHeaderText("No documents found for the specified date range.");
                alert.showAndWait();
            }

        } catch (InterruptedException | ExecutionException | IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("An error occurred while generating the PDF. Please try again.");
            alert.showAndWait();
        }
    }

    private boolean addTable(PDPageContentStream contentStream, List<QueryDocumentSnapshot> documents, DateTimeFormatter formatter, LocalDate start, LocalDate end, PDDocument pdfDocument, Map<String, Integer> vaccineCounts) throws IOException {
        // Create a modifiable list and copy documents into it
        List<QueryDocumentSnapshot> modifiableDocuments = new ArrayList<>(documents);

        // Sort documents by vaccinationDate
        Collections.sort(modifiableDocuments, new Comparator<QueryDocumentSnapshot>() {
            @Override
            public int compare(QueryDocumentSnapshot d1, QueryDocumentSnapshot d2) {
                String dateStr1 = d1.getString("vaccinationDate");
                String dateStr2 = d2.getString("vaccinationDate");
                if (dateStr1 == null || dateStr2 == null) {
                    return 0; // Handle nulls if needed
                }
                LocalDate date1 = LocalDate.parse(dateStr1, formatter);
                LocalDate date2 = LocalDate.parse(dateStr2, formatter);
                return date1.compareTo(date2);
            }
        });

        float yStart = 700;
        float yPosition = yStart;
        float rowHeight = 20f;
        float margin = 20f;

        // Define column widths
        float[] columnWidths = {70, 130, 70, 70, 100, 100, 70};

        // Draw the table headers
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 10);
        contentStream.beginText();
        contentStream.newLineAtOffset(margin, yPosition);
        contentStream.showText("Key");
        contentStream.newLineAtOffset(columnWidths[0], 0);
        contentStream.showText("Vaccine");
        contentStream.newLineAtOffset(columnWidths[1], 0);
        contentStream.showText("Date");
        contentStream.newLineAtOffset(columnWidths[2], 0);
        contentStream.showText("Time");
        contentStream.newLineAtOffset(columnWidths[3], 0);
        contentStream.showText("Patient's Name");
        contentStream.newLineAtOffset(columnWidths[4], 0);
        contentStream.showText("Client Practitioner");
        contentStream.newLineAtOffset(columnWidths[5], 0);
        contentStream.showText("Title");
        contentStream.endText();

        yPosition -= rowHeight;

        // Draw the table data
        contentStream.setFont(PDType1Font.HELVETICA, 8);
        boolean hasData = false;
        for (QueryDocumentSnapshot document : modifiableDocuments) {
            String dateStr = document.getString("vaccinationDate");
            if (dateStr != null) {
                LocalDate docDate = LocalDate.parse(dateStr, formatter);
                if (!docDate.isBefore(start) && !docDate.isAfter(end)) {
                    String vaccine = document.getString("vaccine");
                    vaccineCounts.put(vaccine, vaccineCounts.getOrDefault(vaccine, 0) + 1);
                    contentStream.beginText();
                    contentStream.newLineAtOffset(margin, yPosition);
                    contentStream.showText(document.getString("appointmentKey"));
                    contentStream.newLineAtOffset(columnWidths[0], 0);
                    contentStream.showText(vaccine);
                    contentStream.newLineAtOffset(columnWidths[1], 0);
                    contentStream.showText(dateStr);
                    contentStream.newLineAtOffset(columnWidths[2], 0);
                    contentStream.showText(document.getString("vaccinationTime"));
                    contentStream.newLineAtOffset(columnWidths[3], 0);
                    contentStream.showText(document.getString("client_user"));
                    contentStream.newLineAtOffset(columnWidths[4], 0);
                    contentStream.showText(document.getString("client_practitioner"));
                    contentStream.newLineAtOffset(columnWidths[5], 0);
                    contentStream.showText(document.getString("title"));
                    contentStream.endText();
                    yPosition -= rowHeight;
                    hasData = true;

                    // Add a new page if necessary
                    if (yPosition < margin) {
                        contentStream.close();
                        PDPage newPage = new PDPage();
                        pdfDocument.addPage(newPage);
                        contentStream = new PDPageContentStream(pdfDocument, newPage);

                        yPosition = yStart;
                        // Redraw headers on new page
                        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 10);
                        contentStream.beginText();
                        contentStream.newLineAtOffset(margin, yPosition);
                        contentStream.showText("Key");
                        contentStream.newLineAtOffset(columnWidths[0], 0);
                        contentStream.showText("Vaccine");
                        contentStream.newLineAtOffset(columnWidths[1], 0);
                        contentStream.showText("Date");
                        contentStream.newLineAtOffset(columnWidths[2], 0);
                        contentStream.showText("Time");
                        contentStream.newLineAtOffset(columnWidths[3], 0);
                        contentStream.showText("Patient's Name");
                        contentStream.newLineAtOffset(columnWidths[4], 0);
                        contentStream.showText("Client Practitioner");
                        contentStream.newLineAtOffset(columnWidths[5], 0);
                        contentStream.showText("Title");
                        contentStream.endText();
                        yPosition -= rowHeight;
                    }
                }
            }
        }

        contentStream.close();
        return hasData;
    }

    public static void createSummaryPage(PDDocument document, Map<String, Integer> childVaccineCounts, Map<String, Integer> adultVaccineCounts) {
        PDPage summaryPage = new PDPage();
        document.addPage(summaryPage);

        try (PDPageContentStream summaryContentStream = new PDPageContentStream(document, summaryPage)) {
            float margin = 70;
            float yPosition = summaryPage.getMediaBox().getHeight() - margin;

            // Set font for header
            summaryContentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
            summaryContentStream.beginText();
            summaryContentStream.newLineAtOffset(margin, yPosition);
            summaryContentStream.showText("Vaccine Summary");
            summaryContentStream.endText();

            yPosition -= 20; // Move down after header

            // Set font for body text
            summaryContentStream.setFont(PDType1Font.HELVETICA, 10);

            // Add child vaccines
            summaryContentStream.beginText();
            summaryContentStream.newLineAtOffset(margin, yPosition);
            summaryContentStream.showText("Child Vaccines:");
            summaryContentStream.endText();

            yPosition -= 15; // Move down after title

            for (Map.Entry<String, Integer> entry : childVaccineCounts.entrySet()) {
                summaryContentStream.beginText();
                summaryContentStream.newLineAtOffset(margin + 10, yPosition);
                summaryContentStream.showText("Number of " + entry.getKey() + ": " + entry.getValue());
                summaryContentStream.endText();
                yPosition -= 15; // Move down for next line
            }

            yPosition -= 20; // Add space between sections

            // Add adult vaccines
            summaryContentStream.beginText();
            summaryContentStream.newLineAtOffset(margin, yPosition);
            summaryContentStream.showText("Adult Vaccines:");
            summaryContentStream.endText();

            yPosition -= 15; // Move down after title

            for (Map.Entry<String, Integer> entry : adultVaccineCounts.entrySet()) {
                summaryContentStream.beginText();
                summaryContentStream.newLineAtOffset(margin + 10, yPosition);
                summaryContentStream.showText("Number of " + entry.getKey() + ": " + entry.getValue());
                summaryContentStream.endText();
                yPosition -= 15; // Move down for next line
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addConfidentialityStatement(PDDocument document) throws IOException {
        PDPage lastPage = document.getPage(document.getNumberOfPages() - 1);
        try (PDPageContentStream contentStream = new PDPageContentStream(document, lastPage, AppendMode.APPEND, true)) {
            String[] lines = {
                "Immunicare is committed to protecting sensitive information, including research, patient records, and business data.",
                "Access to this data is restricted to authorized personnel, and unauthorized disclosure is prohibited.",
                "Data must be securely handled and disposed of properly.Any breaches must be reported immediately.",
                "Adherence to these confidentiality practices is essential for maintaining trust and legal compliance."
            };
            InputStream borderStream = getClass().getResourceAsStream("/assets/logo_removebg_preview__1_.png");
            BufferedImage bufferedImageBorder = ImageIO.read(borderStream);
            PDImageXObject imageBorder = LosslessFactory.createFromImage(document, bufferedImageBorder);
            contentStream.drawImage(imageBorder, 460, 0, 140, 70);

            // Set font and initial y-position
            contentStream.setFont(PDType1Font.HELVETICA, 8);
            float yPosition = 50; // Starting position for the confidentiality statement

            // Write each line of the statement
            for (String line : lines) {
                contentStream.beginText();
                contentStream.newLineAtOffset(20, yPosition); // X and Y position for the text
                contentStream.showText(line);
                contentStream.endText();
                yPosition -= 12; // Move down for the next line (adjust as needed)
            }
        }
    }

    @FXML
    private void Back(ActionEvent event) {
        ((Stage) (((Node) event.getSource()).getScene().getWindow())).close();
    }

}
