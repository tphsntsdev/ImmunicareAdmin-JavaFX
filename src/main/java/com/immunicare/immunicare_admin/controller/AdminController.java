package com.immunicare.immunicare_admin.controller;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;

import com.immunicare.immunicare_admin.config.StageManager;
import com.immunicare.immunicare_admin.database.adapter;
import com.immunicare.immunicare_admin.view.FXMLView;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;

@Controller
public class AdminController implements Initializable {

    @FXML
    private AnchorPane displayPane;
    @FXML
    private Button btnDashboard;
    @FXML
    private Button btnCreate;
    @FXML
    private Button btnUpdate;
    @FXML
    private Button btnNews;
    @FXML
    private Button btnLogout;
    @Lazy
    @Autowired
    private StageManager stageManager;

    @FXML
    private void handleCreateAccountAction(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/createaccount.fxml"));
            AnchorPane dashboardPane = loader.load();
            AnchorPane container = displayPane;
            container.getChildren().clear();
            container.getChildren().add(dashboardPane);
            container.setVisible(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleUpdateAccountAction(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/adminaccount.fxml"));
            AnchorPane dashboardPane = loader.load();
            AnchorPane container = displayPane;
            container.getChildren().clear();
            container.getChildren().add(dashboardPane);
            container.setVisible(true);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @FXML
    private void handleDashboardAction(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dashboard.fxml"));
            AnchorPane dashboardPane = loader.load();
            AnchorPane container = displayPane;
            container.getChildren().clear();
            container.getChildren().add(dashboardPane);
            container.setVisible(true);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @FXML
    private void handleCreateNewsAction(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/news.fxml"));
            AnchorPane dashboardPane = loader.load();
            AnchorPane container = displayPane;
            container.getChildren().clear();
            container.getChildren().add(dashboardPane);
            container.setVisible(true);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @FXML
    private void logout(ActionEvent event) {
        if (adapter.getApp() != null) {
            adapter.getApp().delete();
        }
        FXMLView nextView = FXMLView.LOGIN;
        stageManager.switchScene(nextView);

    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        handleDashboardAction(null);
        btnDashboard.setOnMouseEntered(event -> {
            btnDashboard.setStyle("-fx-background-color: #4c8f8f;");
        });
        btnDashboard.setOnMouseExited(event -> {
            btnDashboard.setStyle("-fx-background-color: #66b2b2;");
        });

        btnCreate.setOnMouseEntered(event -> {
            btnCreate.setStyle("-fx-background-color: #4c8f8f;");
        });
        btnCreate.setOnMouseExited(event -> {
            btnCreate.setStyle("-fx-background-color: #66b2b2;");
        });

        btnNews.setOnMouseEntered(event -> {
            btnNews.setStyle("-fx-background-color: #4c8f8f;");
        });
        btnNews.setOnMouseExited(event -> {
            btnNews.setStyle("-fx-background-color: #66b2b2;");
        });

        btnUpdate.setOnMouseEntered(event -> {
            btnUpdate.setStyle("-fx-background-color: #4c8f8f;");
        });
        btnUpdate.setOnMouseExited(event -> {
            btnUpdate.setStyle("-fx-background-color: #66b2b2;");
        });

        btnLogout.setOnMouseEntered(event -> {
            btnLogout.setStyle("-fx-background-color: #4c8f8f;");
        });
        btnLogout.setOnMouseExited(event -> {
            btnLogout.setStyle("-fx-background-color: #66b2b2;");
        });
    }
}
