package com.immunicare.immunicare_admin.controller;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
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
import com.immunicare.immunicare_admin.view.FXMLView;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

@Controller
public class DashboardController implements Initializable {

    Map<String, Integer> childVaccineCounts = new HashMap<>();
    Map<String, Integer> adultVaccineCounts = new HashMap<>();
    private CollectionReference adultVaccineCollection, childVaccineCollection;
    private Firestore db;

    @FXML
    private Button childData;
    @FXML
    private Button adultData;
    @FXML
    private ChoiceBox timeFilter;
    @FXML
    private BarChart adultChart;
    @FXML
    private BarChart childChart;

    String filter_data = "";
    private Stage primaryStage;

    private void initData() {
        db = adapter.getFirestore();
    }

    private String childVaccine_Data, adultVaccine_Data;

    @FXML
    public void generateReport(ActionEvent event) {
        FXMLView nextView = FXMLView.GENERATEREPORT;
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
        stage.show();
    }

    public void generatePdf(String filterData) {
        CollectionReference childVaccineCollection = db.collection("childappointment_data");
        CollectionReference adultVaccineCollection = db.collection("adultappointment_data");
        SimpleDateFormat dateFormat = new SimpleDateFormat("M/d/y");
        Date currentDate = new Date();
        if ("".equals(filterData)) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Error");
            alert.setHeaderText("Dashboard Error");
            alert.setContentText("No Filter Applied, Kindly select a Valid Filter Option");
            alert.showAndWait();
            return;
        }

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

            // Create PDF Document
            PDDocument document = new PDDocument();

            // Add child appointments page
            PDPage childPage = new PDPage();
            document.addPage(childPage);
            PDPageContentStream childContentStream = new PDPageContentStream(document, childPage);
            childContentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
            childContentStream.beginText();
            childContentStream.newLineAtOffset(20, 750);
            childContentStream.showText("Appointments Report - " + filterData + " (Child)");
            childContentStream.endText();
            childContentStream.setFont(PDType1Font.HELVETICA, 10);

            // Add child table
            addTable(childContentStream, childDocuments, dateFormat, currentDate, filterData);
            childContentStream.close();

            // Add adult appointments page
            PDPage adultPage = new PDPage();
            document.addPage(adultPage);
            PDPageContentStream adultContentStream = new PDPageContentStream(document, adultPage);
            adultContentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
            adultContentStream.beginText();
            adultContentStream.newLineAtOffset(20, 750);
            adultContentStream.showText("Appointments Report - " + filterData);
            adultContentStream.endText();
            adultContentStream.setFont(PDType1Font.HELVETICA, 10);

            // Add adult table
            addTable(adultContentStream, adultDocuments, dateFormat, currentDate, filterData);
            adultContentStream.close();
            createSummaryPage(document, childVaccineCounts, adultVaccineCounts);
            addConfidentialityStatement(document);

            // Save the document
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save PDF");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
            File file = fileChooser.showSaveDialog(primaryStage);

            if (file != null) {
                document.save(file);
                System.out.println("PDF generated successfully at " + file.getAbsolutePath());
            }

            document.close();

        } catch (InterruptedException | ExecutionException | ParseException | IOException e) {
            e.printStackTrace();
        }
    }

    private void addTable(PDPageContentStream contentStream, List<QueryDocumentSnapshot> documents, SimpleDateFormat dateFormat, Date currentDate, String filterData) throws IOException, ParseException {
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
        List<QueryDocumentSnapshot> modifiableDocuments = new ArrayList<>(documents);
        Collections.sort(modifiableDocuments, new Comparator<QueryDocumentSnapshot>() {
            @Override
            public int compare(QueryDocumentSnapshot doc1, QueryDocumentSnapshot doc2) {
                try {
                    String dateStr1 = doc1.getString("vaccinationDate");
                    String dateStr2 = doc2.getString("vaccinationDate");
                    Date date1 = dateFormat.parse(dateStr1);
                    Date date2 = dateFormat.parse(dateStr2);
                    return date2.compareTo(date1); // Descending order
                } catch (ParseException e) {
                    e.printStackTrace();
                    return 0;
                }
            }
        });

        // Draw the table data based on the filter
        contentStream.setFont(PDType1Font.HELVETICA, 8);
        for (QueryDocumentSnapshot document : documents) {
            String dateStr = document.getString("vaccinationDate");
            Date vaccinationDate = dateFormat.parse(dateStr);
            if (shouldIncludeDate(vaccinationDate, currentDate, filterData)) {
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText(document.getString("appointmentKey"));
                contentStream.newLineAtOffset(columnWidths[0], 0);
                contentStream.showText(document.getString("vaccine"));
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
            }
        }
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

    private boolean shouldIncludeDate(Date vaccinationDate, Date currentDate, String filterData) {

        switch (filterData) {
            case "Daily":
                return isWithinDay(vaccinationDate, currentDate);
            case "Weekly":
                return isWithinWeek(vaccinationDate, currentDate);
            case "Monthly":
                return isWithinMonth(vaccinationDate, currentDate);
            default:
                return false;
        }
    }

    @FXML
    public void Print(ActionEvent event) {
        generatePdf(filter_data);
    }

    @FXML
    private void dataDisplay(String text_time) {
        adultVaccineCollection = db.collection("adultappointment_data");
        childVaccineCollection = db.collection("childappointment_data");
        childVaccine_Data = "";
        adultVaccine_Data = "";
        Date currentDate = new Date();
        XYChart.Series<String, Number> adultSeries = new XYChart.Series<>();
        XYChart.Series<String, Number> childSeries = new XYChart.Series<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("M/d/y");

        try {
            ApiFuture<QuerySnapshot> child_future = childVaccineCollection.get();
            List<QueryDocumentSnapshot> child_documents = child_future.get().getDocuments();
            ApiFuture<QuerySnapshot> adult_future = adultVaccineCollection.get();
            List<QueryDocumentSnapshot> adult_documents = adult_future.get().getDocuments();

            if ("Daily".equals(text_time)) {
                childVaccine_Data = "";
                adultVaccine_Data = "";
                childSeries.getData().clear();
                adultSeries.getData().clear();
                adultVaccineCounts.clear();
                childVaccineCounts.clear();
                for (QueryDocumentSnapshot document : child_documents) {
                    String dateStr = document.getString("vaccinationDate");
                    Date vaccinationDate = dateFormat.parse(dateStr);
                    if (isWithinDay(vaccinationDate, currentDate)) {
                        String vaccineString = document.getString("vaccine");
                        childVaccineCounts.put(vaccineString, childVaccineCounts.getOrDefault(vaccineString, 0) + 1);
                    }
                }
                for (QueryDocumentSnapshot document : adult_documents) {
                    String dateStr = document.getString("vaccinationDate");
                    Date vaccinationDate = dateFormat.parse(dateStr);
                    if (isWithinDay(vaccinationDate, currentDate)) {
                        String vaccineString = document.getString("vaccine");
                        adultVaccineCounts.put(vaccineString, adultVaccineCounts.getOrDefault(vaccineString, 0) + 1);
                    }
                }

                for (String vaccineString : childVaccineCounts.keySet()) {
                    int count = childVaccineCounts.get(vaccineString);
                    childSeries.getData().add(new XYChart.Data<>(vaccineString, count));
                    childVaccine_Data += "Number of " + vaccineString + ": " + count + "\n";
                }
                for (String vaccineString : adultVaccineCounts.keySet()) {
                    int count = adultVaccineCounts.get(vaccineString);
                    adultSeries.getData().add(new XYChart.Data<>(vaccineString, count));
                    adultVaccine_Data += "Number of " + vaccineString + ": " + count + "\n";
                }

            }
            if ("Weekly".equals(text_time)) {
                childVaccine_Data = "";
                adultVaccine_Data = "";
                adultSeries.getData().clear();
                childSeries.getData().clear();
                childVaccineCounts.clear();
                adultVaccineCounts.clear();
                for (QueryDocumentSnapshot document : child_documents) {
                    String dateStr = document.getString("vaccinationDate");
                    Date vaccinationDate = dateFormat.parse(dateStr);
                    if (isWithinWeek(vaccinationDate, currentDate)) {
                        String vaccineString = document.getString("vaccine");
                        childVaccineCounts.put(vaccineString, childVaccineCounts.getOrDefault(vaccineString, 0) + 1);
                    }
                }
                for (QueryDocumentSnapshot document : adult_documents) {
                    String dateStr = document.getString("vaccinationDate");
                    Date vaccinationDate = dateFormat.parse(dateStr);
                    if (isWithinWeek(vaccinationDate, currentDate)) {
                        String vaccineString = document.getString("vaccine");
                        adultVaccineCounts.put(vaccineString, adultVaccineCounts.getOrDefault(vaccineString, 0) + 1);
                    }
                }

                // Add data points to the series
                for (String vaccineString : childVaccineCounts.keySet()) {
                    int count = childVaccineCounts.get(vaccineString);
                    childSeries.getData().add(new XYChart.Data<>(vaccineString, count));
                    childVaccine_Data += "Number of " + vaccineString + ": " + count + "\n";
                }
                for (String vaccineString : adultVaccineCounts.keySet()) {
                    int count = adultVaccineCounts.get(vaccineString);
                    adultSeries.getData().add(new XYChart.Data<>(vaccineString, count));
                    adultVaccine_Data += "Number of " + vaccineString + ": " + count + "\n";
                }

            }
            if ("Monthly".equals(text_time)) {
                childVaccine_Data = "";
                adultVaccine_Data = "";
                adultSeries.getData().clear();
                childSeries.getData().clear();
                adultVaccineCounts.clear();
                childVaccineCounts.clear();
                for (QueryDocumentSnapshot document : child_documents) {
                    String dateStr = document.getString("vaccinationDate");
                    Date vaccinationDate = dateFormat.parse(dateStr);
                    if (isWithinMonth(vaccinationDate, currentDate)) {
                        String vaccineString = document.getString("vaccine");
                        childVaccineCounts.put(vaccineString, childVaccineCounts.getOrDefault(vaccineString, 0) + 1);
                    }
                }
                for (QueryDocumentSnapshot document : adult_documents) {
                    String dateStr = document.getString("vaccinationDate");
                    Date vaccinationDate = dateFormat.parse(dateStr);
                    if (isWithinMonth(vaccinationDate, currentDate)) {
                        String vaccineString = document.getString("vaccine");
                        adultVaccineCounts.put(vaccineString, adultVaccineCounts.getOrDefault(vaccineString, 0) + 1);
                    }
                }

                // Add data points to the series
                for (String vaccineString : childVaccineCounts.keySet()) {
                    int count = childVaccineCounts.get(vaccineString);
                    childSeries.getData().add(new XYChart.Data<>(vaccineString, count));
                    childVaccine_Data += "Number of " + vaccineString + ": " + count + "\n";
                }
                for (String vaccineString : adultVaccineCounts.keySet()) {
                    int count = adultVaccineCounts.get(vaccineString);
                    adultSeries.getData().add(new XYChart.Data<>(vaccineString, count));
                    adultVaccine_Data += "Number of " + vaccineString + ": " + count + "\n";
                }

            }
            adultChart.getData().clear();
            adultChart.getData().add(adultSeries);
            childChart.getData().clear();
            childChart.getData().add(childSeries);
        } catch (InterruptedException | ExecutionException | ParseException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initData();
        ObservableList<String> options = FXCollections.observableArrayList("Daily", "Weekly", "Monthly");
        timeFilter.setItems(options);
        timeFilter.setValue("");
        timeFilter.valueProperty().addListener((obs, oldValue, newValue) -> {
            filter_data = (String) newValue;
        });
        dataDisplay(filter_data);

        timeFilter.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                dataDisplay(newValue.toString());
                CategoryAxis adultAxis = (CategoryAxis) adultChart.getXAxis();
                CategoryAxis childAxis = (CategoryAxis) childChart.getXAxis();
                adultAxis.setTickLabelsVisible(true);
                childAxis.setTickLabelsVisible(true);
                adultChart.setAnimated(false);
                childChart.setAnimated(false);
                NumberAxis adultYAxis = (NumberAxis) adultChart.getYAxis();
                NumberAxis childYAxis = (NumberAxis) childChart.getYAxis();
                adultYAxis.setAutoRanging(true);
                childYAxis.setAutoRanging(true);
                adultAxis.setTickLabelRotation(45);
                childAxis.setTickLabelRotation(45);
                adultChart.setLegendVisible(false);
                childChart.setLegendVisible(false);
                adultChart.setMaxWidth(Double.MAX_VALUE);
                adultChart.setMaxHeight(Double.MAX_VALUE);
                childChart.setMaxWidth(Double.MAX_VALUE);
                childChart.setMaxHeight(Double.MAX_VALUE);

            }
        });
    }

    private static boolean isWithinDay(Date documentDate, Date currentDate) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(documentDate);
        cal2.setTime(currentDate);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
                && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    private static boolean isWithinWeek(Date documentDate, Date currentDate) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(documentDate);
        cal2.setTime(currentDate);

        cal1.setFirstDayOfWeek(Calendar.MONDAY);
        cal2.setFirstDayOfWeek(Calendar.MONDAY);

        int week1 = cal1.get(Calendar.WEEK_OF_YEAR);
        int week2 = cal2.get(Calendar.WEEK_OF_YEAR);

        int year1 = cal1.get(Calendar.YEAR);
        int year2 = cal2.get(Calendar.YEAR);

        return week1 == week2 && year1 == year2;
    }

    private static boolean isWithinMonth(Date documentDate, Date currentDate) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(documentDate);
        cal2.setTime(currentDate);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
                && cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH);
    }

    @FXML
    public void DisplayChildData(ActionEvent event) {
        if ("".equals(filter_data)) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Error");
            alert.setHeaderText("Dashboard Error");
            alert.setContentText("No Filter Applied, Kindly select a Valid Filter Option");
            alert.showAndWait();
        } else {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Child Vaccine Data");
            alert.setHeaderText("Child Vaccine Count Summary");
            alert.setContentText(childVaccine_Data);
            alert.showAndWait();
        }

    }

    @FXML
    public void DisplayAdultData(ActionEvent event) {
        if ("".equals(filter_data)) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Error");
            alert.setHeaderText("Dashboard Error");
            alert.setContentText("No Filter Applied, Kindly select a Valid Filter Option");
            alert.showAndWait();
        } else {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Adult Vaccine Data");
            alert.setHeaderText("Adult Vaccine Count Summary");
            alert.setContentText(adultVaccine_Data);
            alert.showAndWait();

        }

    }

}
