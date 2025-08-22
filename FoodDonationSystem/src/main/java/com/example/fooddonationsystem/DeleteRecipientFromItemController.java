package com.example.fooddonationsystem;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;

/**
 * Controller for removing (undoing) an assignment of an item to a recipient.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Load distributions filtered by donation and display them in a table.</li>
 *   <li>Allow selection of a distribution row to pre-fill the form fields.</li>
 *   <li>Validate input and call {@link dbManagement#deleteRecipientFromDonation(int, int)}.</li>
 *   <li>Show feedback and refresh the table to reflect changes.</li>
 * </ul>
 */
public class DeleteRecipientFromItemController {

    /** Default constructor required by FXMLLoader (doclint-friendly). */
    public DeleteRecipientFromItemController() { }

    // ===== Top controls =====

    /**
     * @FXML ComboBox injected from FXML.
     * Holds donation selections in format {@code "ID - Name"} (e.g., {@code "1 - Emergency Food Aid"}).
     * Changing the value refreshes the distributions table.
     */
    @FXML private ComboBox<String> donation;

    // ===== Distributions table =====

    /** @FXML TableView injected from FXML. Displays current item→recipient distributions. */
    @FXML private TableView<RowVM> assignmentsTable;

    /** @FXML TableColumn injected from FXML. Item ID column. */
    @FXML private TableColumn<RowVM, Number> colItemId;

    /** @FXML TableColumn injected from FXML. Item name column. */
    @FXML private TableColumn<RowVM, String>  colItemName;

    /** @FXML TableColumn injected from FXML. Recipient ID column. */
    @FXML private TableColumn<RowVM, Number> colRecipientId;

    /** @FXML TableColumn injected from FXML. Recipient name column. */
    @FXML private TableColumn<RowVM, String>  colRecipientName;

    /** @FXML TableColumn injected from FXML. Quantity column. */
    @FXML private TableColumn<RowVM, Number> colQty;

    /** @FXML TableColumn injected from FXML. Distribution date column (displayed as String). */
    @FXML private TableColumn<RowVM, String>  colDate;

    // ===== Bottom form =====

    /**
     * @FXML TextField injected from FXML.
     * Item ID to delete from (auto-filled when clicking a table row, or entered manually).
     */
    @FXML private TextField itemIdField;

    /**
     * @FXML TextField injected from FXML.
     * Recipient ID to remove from the selected item (auto-filled on row click or entered manually).
     */
    @FXML private TextField recipientID;

    /**
     * @FXML Label injected from FXML.
     * Displays success or error feedback for the delete action.
     */
    @FXML private Label wrongDeleteRecipientFromItem;

    /** Backing list for {@link #assignmentsTable}. */
    private final ObservableList<RowVM> rows = FXCollections.observableArrayList();

    /**
     * Initializes the controller after FXML loading.
     * <ul>
     *   <li>Populates the donation picker.</li>
     *   <li>Sets up table columns and binds the backing list.</li>
     *   <li>Wires row selection to pre-fill Item ID and Recipient ID fields.</li>
     *   <li>Loads initial distributions.</li>
     * </ul>
     */
    @FXML
    public void initialize() {
        // donations
        donation.getItems().clear();
        for (Object[] d : dbManagement.getDonationsRaw()) {
            donation.getItems().add(d[0] + " - " + String.valueOf(d[1]));
        }
        if (!donation.getItems().isEmpty()) donation.setValue(donation.getItems().get(0));

        // numeric-only hint for Recipient ID
        recipientID.setPromptText("e.g. 1");

        // table
        colItemId.setCellValueFactory(d -> d.getValue().itemIdProperty());
        colItemName.setCellValueFactory(d -> d.getValue().itemNameProperty());
        colRecipientId.setCellValueFactory(d -> d.getValue().recipientIdProperty());
        colRecipientName.setCellValueFactory(d -> d.getValue().recipientNameProperty());
        colQty.setCellValueFactory(d -> d.getValue().qtyProperty());
        colDate.setCellValueFactory(d -> d.getValue().dateProperty());
        assignmentsTable.setItems(rows);

        // click a row to fill fields
        assignmentsTable.getSelectionModel().selectedItemProperty().addListener((obs,o,n)->{
            if (n!=null) {
                itemIdField.setText(String.valueOf(n.getItemId()));
                recipientID.setText(String.valueOf(n.getRecipientId()));
            }
        });

        refresh();
    }

    /**
     * FXML handler for donation ComboBox changes.
     * Reloads the distributions table for the newly selected donation.
     */
    @FXML
    public void onDonationChange() { refresh(); }

    /**
     * Parses the selected donation ID from the donation picker.
     *
     * @return selected donation ID, or {@code null} if not parsable/selected.
     */
    private Integer selectedDonationId() {
        if (donation.getValue()==null) return null;
        String s = donation.getValue();
        int dash = s.indexOf(" - ");
        if (dash<=0) return null;
        try { return Integer.parseInt(s.substring(0,dash).trim()); }
        catch (Exception e) { return null; }
    }

    /**
     * Refreshes the table with distributions for the selected donation (or all, if none).
     * Populates {@link #rows} with {@link RowVM} instances.
     */
    private void refresh() {
        rows.clear();
        Integer did = selectedDonationId();
        for (Object[] r : dbManagement.getDistributionsRaw(did)) {
            // item_id, item_name, recipient_id, recipient_name, qty, date, donation_id
            int itemId = (int) r[0];
            String itemName = String.valueOf(r[1]);
            int recId = (int) r[2];
            String recName = String.valueOf(r[3]);
            int qty = (int) r[4];
            String date = String.valueOf(r[5]);
            rows.add(new RowVM(itemId, itemName, recId, recName, qty, date));
        }
    }

    /**
     * FXML handler that removes an assignment between an item and a recipient.
     * <p>
     * Flow:
     * <ol>
     *   <li>Validate that Item ID and Recipient ID are integers.</li>
     *   <li>Call {@link dbManagement#deleteRecipientFromDonation(int, int)}.</li>
     *   <li>Show success/error message; refresh the table if successful.</li>
     * </ol>
     *
     * @param event action event from the "Delete" button.
     */
    @FXML
    public void deleteRecipientFromItem(ActionEvent event) {
        wrongDeleteRecipientFromItem.setTextFill(Color.RED);
        wrongDeleteRecipientFromItem.setText("");

        int itemId;
        int recId;
        try { itemId = Integer.parseInt(itemIdField.getText().trim()); }
        catch (Exception e) { wrongDeleteRecipientFromItem.setText("Enter a valid Item ID."); return; }
        try { recId = Integer.parseInt(recipientID.getText().trim()); }
        catch (Exception e) { wrongDeleteRecipientFromItem.setText("Enter a valid Recipient ID."); return; }

        String result = dbManagement.deleteRecipientFromDonation(itemId, recId);
        boolean ok = result.toLowerCase().contains("success");
        wrongDeleteRecipientFromItem.setTextFill(ok ? Color.GREEN : Color.RED);
        wrongDeleteRecipientFromItem.setText(result);

        if (ok) {
            refresh(); // show the change immediately
            recipientID.clear();
        }
    }

    /**
     * FXML handler to clear the form and table selection.
     *
     * @param event action event from the "Clear" button.
     */
    @FXML
    public void clear(ActionEvent event) {
        itemIdField.clear();
        recipientID.clear();
        wrongDeleteRecipientFromItem.setText("");
        assignmentsTable.getSelectionModel().clearSelection();
    }

    // ===== View Model =====

    /**
     * Lightweight view model representing a single distribution row
     * (item→recipient assignment) shown in {@link #assignmentsTable}.
     * Uses JavaFX properties for table binding.
     */
    public static class RowVM {
        private final IntegerProperty itemId = new SimpleIntegerProperty();
        private final StringProperty itemName = new SimpleStringProperty();
        private final IntegerProperty recipientId = new SimpleIntegerProperty();
        private final StringProperty recipientName = new SimpleStringProperty();
        private final IntegerProperty qty = new SimpleIntegerProperty();
        private final StringProperty date = new SimpleStringProperty();

        /**
         * Constructs a view model instance.
         *
         * @param itemId    item identifier.
         * @param itemName  item display name.
         * @param recId     recipient identifier.
         * @param recName   recipient display name.
         * @param qty       quantity distributed.
         * @param date      distribution date (string-formatted).
         */
        public RowVM(int itemId, String itemName, int recId, String recName, int qty, String date) {
            this.itemId.set(itemId);
            this.itemName.set(itemName);
            this.recipientId.set(recId);
            this.recipientName.set(recName);
            this.qty.set(qty);
            this.date.set(date);
        }

        /** Returns the JavaFX property for item ID. @return the item ID property. */
        public IntegerProperty itemIdProperty(){ return itemId; }
        /** Returns the JavaFX property for item name. @return the item name property. */
        public StringProperty itemNameProperty(){ return itemName; }
        /** Returns the JavaFX property for recipient ID. @return the recipient ID property. */
        public IntegerProperty recipientIdProperty(){ return recipientId; }
        /** Returns the JavaFX property for recipient name. @return the recipient name property. */
        public StringProperty recipientNameProperty(){ return recipientName; }
        /** Returns the JavaFX property for quantity. @return the quantity property. */
        public IntegerProperty qtyProperty(){ return qty; }
        /** Returns the JavaFX property for date string. @return the date string property. */
        public StringProperty dateProperty(){ return date; }

        /** Returns the primitive item ID value. @return the item ID. */
        public int getItemId(){ return itemId.get(); }
        /** Returns the primitive recipient ID value. @return the recipient ID. */
        public int getRecipientId(){ return recipientId.get(); }
    }
}
