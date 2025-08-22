package com.example.fooddonationsystem;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;

/**
 * Controller for viewing items a specific recipient has received.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Accept a Recipient ID (digits only) and trigger a lookup.</li>
 *   <li>Query the DB via {@link dbManagement#getRecipientItems(int)}.</li>
 *   <li>Display item rows (item id/name, donation name, qty, date) in a table.</li>
 *   <li>Show inline status messages (success/empty/error).</li>
 * </ul>
 * UX notes:
 * <ul>
 *   <li>Pressing ENTER in the Recipient ID field triggers the lookup.</li>
 *   <li>Use the Clear button to reset the input and table.</li>
 * </ul>
 */
public class ViewRecipientsItemController {

    // ===== Top filter =====

    /**
     * @FXML TextField injected from FXML.
     * Numeric-only Recipient ID. ENTER key triggers {@link #viewItems(ActionEvent)}.
     */
    @FXML private TextField recipientID;

    // ===== Table =====

    /** @FXML TableView injected from FXML. Displays items assigned to the recipient. */
    @FXML private TableView<RowVM> table;

    /** @FXML TableColumn injected from FXML. Item ID column. */
    @FXML private TableColumn<RowVM, Number> colItemId;

    /** @FXML TableColumn injected from FXML. Item name column. */
    @FXML private TableColumn<RowVM, String>  colItemName;

    /** @FXML TableColumn injected from FXML. Donation name column. */
    @FXML private TableColumn<RowVM, String>  colDonation;

    /** @FXML TableColumn injected from FXML. Quantity column. */
    @FXML private TableColumn<RowVM, Number> colQty;

    /** @FXML TableColumn injected from FXML. Distribution date column. */
    @FXML private TableColumn<RowVM, String>  colDate;

    // ===== Status =====

    /** @FXML Label injected from FXML. Shows status text (green for info/success, red for errors). */
    @FXML private Label statusLabel;

    /** Backing list bound to {@link #table}. */
    private final ObservableList<RowVM> rows = FXCollections.observableArrayList();

    /**
     * Initializes input formatters, ENTER key behavior, table column bindings,
     * and clears the status message.
     */
    public void initialize() {
        // numeric-only recipient ID
        if (recipientID != null) {
            recipientID.setTextFormatter(new TextFormatter<>(c ->
                    c.getControlNewText().matches("\\d{0,9}") ? c : null));
            recipientID.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.ENTER) viewItems(null);
            });
        }

        // table wiring
        if (table != null) {
            colItemId.setCellValueFactory(d -> d.getValue().itemIdProperty());
            colItemName.setCellValueFactory(d -> d.getValue().itemNameProperty());
            colDonation.setCellValueFactory(d -> d.getValue().donationProperty());
            colQty.setCellValueFactory(d -> d.getValue().qtyProperty());
            colDate.setCellValueFactory(d -> d.getValue().dateProperty());
            table.setItems(rows);
        }

        setStatus("", false);
    }

    /**
     * FXML handler that retrieves and displays items for the entered Recipient ID.
     * <p>
     * Flow:
     * <ol>
     *   <li>Parse {@link #recipientID} as an integer.</li>
     *   <li>Call {@link dbManagement#getRecipientItems(int)}.</li>
     *   <li>Populate the table and display aggregate info (row count + total qty), or
     *       show a "No items found" status.</li>
     * </ol>
     *
     * @param event action event (may be {@code null} when invoked via ENTER).
     */
    @FXML
    public void viewItems(ActionEvent event) {
        setStatus("", false);
        rows.clear();

        final int rid;
        try {
            rid = Integer.parseInt(recipientID.getText().trim());
        } catch (Exception ex) {
            setStatus("Please enter a valid Recipient ID (integer).", true);
            return;
        }

        var data = dbManagement.getRecipientItems(rid);
        if (data == null || data.isEmpty()) {
            setStatus("No items found for recipient " + rid + ".", true);
            return;
        }

        int total = 0;
        for (Object[] r : data) {
            int    itemId   = (r[0] instanceof Number) ? ((Number) r[0]).intValue() : safeInt(r[0]);
            String name     = r[1] == null ? "" : String.valueOf(r[1]);
            String donation = r[2] == null ? "" : String.valueOf(r[2]);
            int    qty      = (r[3] instanceof Number) ? ((Number) r[3]).intValue() : safeInt(r[3]);
            String date     = r[4] == null ? "" : String.valueOf(r[4]);

            rows.add(new RowVM(itemId, name, donation, qty, date));
            total += qty;
        }
        setStatus("Found " + rows.size() + " item rows (total qty: " + total + ").", false);
    }

    /**
     * FXML handler to clear the Recipient ID field, table content, and status message.
     *
     * @param event action event from the "Clear" button.
     */
    @FXML
    public void clear(ActionEvent event) {
        if (recipientID != null) recipientID.clear();
        rows.clear();
        setStatus("", false);
    }

    /**
     * Updates the status label with a message styled as error (red) or info/success (green).
     *
     * @param msg   message text.
     * @param error whether the message indicates an error.
     */
    private void setStatus(String msg, boolean error) {
        if (statusLabel != null) {
            statusLabel.setTextFill(error ? Color.RED : Color.GREEN);
            statusLabel.setText(msg);
        }
    }

    /**
     * Safely parses an integer from an arbitrary object; returns 0 on failure.
     *
     * @param o object to parse.
     * @return integer value or 0 if parsing fails.
     */
    private int safeInt(Object o) {
        try { return Integer.parseInt(String.valueOf(o)); } catch (Exception e) { return 0; }
    }

    // ===== Table Row View-Model =====

    /**
     * Lightweight view model representing a single "item received by recipient" row.
     * Wraps values in JavaFX properties for table binding.
     */
    public static class RowVM {
        private final IntegerProperty itemId = new SimpleIntegerProperty();
        private final StringProperty  itemName = new SimpleStringProperty();
        private final StringProperty  donation = new SimpleStringProperty();
        private final IntegerProperty qty = new SimpleIntegerProperty();
        private final StringProperty  date = new SimpleStringProperty();

        /**
         * Constructs a row view-model.
         *
         * @param itemId    item identifier.
         * @param itemName  item display name.
         * @param donation  donation name/label.
         * @param qty       quantity received.
         * @param date      distribution date (string).
         */
        public RowVM(int itemId, String itemName, String donation, int qty, String date) {
            this.itemId.set(itemId);
            this.itemName.set(itemName);
            this.donation.set(donation);
            this.qty.set(qty);
            this.date.set(date);
        }

        /** @return property for item ID. */
        public IntegerProperty itemIdProperty() { return itemId; }
        /** @return property for item name. */
        public StringProperty  itemNameProperty() { return itemName; }
        /** @return property for donation label. */
        public StringProperty  donationProperty() { return donation; }
        /** @return property for quantity. */
        public IntegerProperty qtyProperty() { return qty; }
        /** @return property for date string. */
        public StringProperty  dateProperty() { return date; }
    }
}
