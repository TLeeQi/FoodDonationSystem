package com.example.fooddonationsystem;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.Pane;

import java.io.IOException;

public class MainController {

    @FXML
    private Pane contentPane;

    public void initialize() {
        loadPage("welcome.fxml");
    }

    @FXML
    private void loadAddItem(ActionEvent event) {
        loadPage("addItem.fxml");
    }

    @FXML
    private void loadDeleteItem(ActionEvent event) {
        loadPage("deleteItem.fxml");
    }

    @FXML
    private void loadAssignItems(ActionEvent event) {
        loadPage("addRecipientToItem.fxml");
    }

    @FXML
    private void loadRemoveAssignment(ActionEvent event) {
        loadPage("deleteRecipientFromItem.fxml");
    }

    @FXML
    private void loadUpdateStock(ActionEvent event) {
        loadPage("updateStock.fxml");
    }

    @FXML
    private void loadViewRecipientsInItem(ActionEvent event) {
        loadPage("viewDistribution.fxml");
    }

    @FXML
    private void loadViewRecipientsItem(ActionEvent event) {
        loadPage("viewRecipientsItem.fxml");
    }

    @FXML
    private void loadViewDistribution(ActionEvent event) {
        loadPage("viewDistribution.fxml");
    }

    @FXML
    private void loadLogOut(ActionEvent event) {
        loadPage("logout.fxml");
    }

    private void loadPage(String fxmlFileName) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFileName));
            Parent page = loader.load();
            contentPane.getChildren().setAll(page);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}