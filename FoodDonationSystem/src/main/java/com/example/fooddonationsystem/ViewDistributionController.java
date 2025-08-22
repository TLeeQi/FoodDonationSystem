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
 * Controller for viewing item distribution records.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Optionally filter by donation (via picker) and/or by specific item ID.</li>
 *   <li>Query the DB through {@link dbManagement#getItemDistributions(Integer, Integer)}.</li>
 *   <li>Display results in a table and show aggregate feedback.</li>
 * </ul>
 * <p>
 * UX notes:
 * <ul>
 *   <li>The donation picker includes an "All Donations" option.</li>
 *   <li>{@code itemNumber} accepts only digits; pressing ENTER triggers a refresh.</li>
 * </ul>
 */
public class ViewDistributionController {

    // ===== Top filters =====

    /**
     * @FXML ComboBox injected from FXML.
     * Holds donation entries in the form {@code "ID - Name"}, plus an "All Donations" option.
     */
    @FXML private ComboBox<String> donationPicker;

    /**
     * @FXML TextField injected from FXML.
     * Optional numeric filter for a specific Item ID; ENTER key triggers refresh.
     */
    @FXML private TextField itemNumber;

    // ===== Table =====

    /** @FXML TableView injected from FXML. Displays matching distribution rows. */
    @FXML private TableView<RowVM> table;

    /** @FXML TableColumn injected from FXML. Recipient ID column. */
    @FXML private TableColumn<RowVM, Number> colRecipientId;

    /** @FXML TableColumn injected from FXML. Recipient name column. */
    @FXML private TableColumn<RowVM, String>  colRecipientName;

    /** @FXML TableColumn injected from FXML. Donation name column. */
    @FXML private TableColumn<RowVM, String>  colDonation;

    /** @FXML TableColumn injected from FXML. Quantity column. */
    @FXML private TableColumn<RowVM, Number> colQty;

    /** @FXML TableColumn injected from FXML. Distribution date column. */
    @FXML private TableColumn<RowVM, String>  colDate;

    // ===== Status =====

    /** @FXML Label injected from FXML. Shows success/empty/error status messages. */
    @FXML private Label statusLabel;

    /** Backing list for {@link #table}. */
    private final ObservableList<RowVM> rows = FXCollections.observableArrayList();

    /**
     * Initializes controls, loads donations list, wires handlers, and performs an initial query.
     * <ul>
     *   <li>Adds "All Donations" and DB-driven donation entries to the picker.</li>
     *   <li>Applies a digits-only formatter to {@link #itemNumber} and ENTER-to-refresh behavior.</li>
     *   <li>Configures table column bindings.</li>
     * </ul>
     */
    public void initialize() {
        viewDistributions(null);

        // Donation picker list from DB (+ All)
        if (donationPicker != null) {
            donationPicker.getItems().add("All Donations");
            for (Object[] d : dbManagement.getDonationsRaw()) {
                donationPicker.getItems().add(d[0] + " - " + String.valueOf(d[1]));
            }
            donationPicker.getSelectionModel().selectFirst();
        }

        // numeric item ID + ENTER to trigger
        if (itemNumber != null) {
            itemNumber.setTextFormatter(new TextFormatter<>(c ->
                    c.getControlNewText().matches("\\d{0,9}") ? c : null));
            itemNumber.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.ENTER) viewDistributions(null);
            });
        }

        // table wiring
        colRecipientId.setCellValueFactory(d -> d.getValue().recipientIdProperty());
        colRecipientName.setCellValueFactory(d -> d.getValue().recipientNameProperty());
        colDonation.setCellValueFactory(d -> d.getValue().donationProperty());
        colQty.setCellValueFactory(d -> d.getValue().qtyProperty());
        colDate.setCellValueFactory(d -> d.getValue().dateProperty());
        table.setItems(rows);

        setStatus("", false);
    }

    /**
     * FXML handler to query and display distributions using current filters.
     * <p>
     * Flow:
     * <ol>
     *   <li>Parse optional Item ID from {@link #itemNumber} (digits only).</li>
     *   <li>Parse optional Donation ID from {@link #donationPicker}.</li>
     *   <li>Call {@link dbManagement#getItemDistributions(Integer, Integer)} with those filters.</li>
     *   <li>Populate the table and show an aggregate status (row count + total qty), or
     *       show a "No distributions found" message.</li>
     * </ol>
     *
     * @param e action event (may be {@code null} when called programmatically).
     */
    @FXML
    public void viewDistributions(ActionEvent e) {
        setStatus("", false);
        rows.clear();

        // Item ID is optional
        Integer itemId = null;
        String raw = (itemNumber == null) ? null : itemNumber.getText();
        if (raw != null && !raw.trim().isEmpty()) {
            try {
                itemId = Integer.parseInt(raw.trim());
            } catch (NumberFormatException ex) {
                setStatus("Item ID must be an integer (or leave blank).", true);
                return;
            }
        }

        Integer donationId = parseSelectedDonationId();

        var data = dbManagement.getItemDistributions(itemId, donationId);
        if (data == null || data.isEmpty()) {
            String where = (donationId == null ? "all donations" : "donation " + donationId);
            where += (itemId == null ? "" : (", item " + itemId));
            setStatus("No distributions found for " + where + ".", true);
            return;
        }

        int total = 0;
        for (Object[] r : data) {
            int    rid   = safeInt(r[0]);
            String name  = r[1] == null ? "" : String.valueOf(r[1]);
            String don   = r[2] == null ? "" : String.valueOf(r[2]);
            int    qty   = safeInt(r[3]);
            String date  = r[4] == null ? "" : String.valueOf(r[4]);

            rows.add(new RowVM(rid, name, don, qty, date));
            total += qty;
        }
        setStatus("Found " + rows.size() + " rows (total qty: " + total + ").", false);
    }

    /**
     * FXML handler to clear filters, table content, and status message.
     *
     * @param e action event from the "Clear" button.
     */
    @FXML
    public void clear(ActionEvent e) {
        if (itemNumber != null) itemNumber.clear();
        rows.clear();
        setStatus("", false);
        if (donationPicker != null) donationPicker.getSelectionModel().selectFirst(); // back to "All"
    }

    /**
     * Parses the selected donation ID from {@link #donationPicker}.
     *
     * @return donation ID, or {@code null} if "All Donations" or unparsable.
     */
    private Integer parseSelectedDonationId() {
        if (donationPicker == null) return null;
        String val = donationPicker.getValue();
        if (val == null || val.equals("All Donations")) return null;
        int dash = val.indexOf(" - ");
        if (dash <= 0) return null;
        try { return Integer.parseInt(val.substring(0, dash).trim()); }
        catch (NumberFormatException e) { return null; }
    }

    /**
     * Updates {@link #statusLabel} with a message in red (error) or green (success/info).
     *
     * @param msg   text to display.
     * @param error whether to display as error (red) or success/info (green).
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
     * @return parsed integer or 0 if parsing fails.
     */
    private int safeInt(Object o) {
        try { return Integer.parseInt(String.valueOf(o)); } catch (Exception e) { return 0; }
    }

    // ===== Table Row View-Model =====

    /**
     * Lightweight view model for a single distribution row.
     * Wraps fields in JavaFX properties for table binding.
     */
    public static class RowVM {
        private final IntegerProperty recipientId = new SimpleIntegerProperty();
        private final StringProperty  recipientName = new SimpleStringProperty();
        private final StringProperty  donation = new SimpleStringProperty();
        private final IntegerProperty qty = new SimpleIntegerProperty();
        private final StringProperty  date = new SimpleStringProperty();

        /**
         * Constructs a row view-model.
         *
         * @param recipientId  recipient identifier.
         * @param recipientName recipient display name.
         * @param donation     donation name/label.
         * @param qty          quantity distributed.
         * @param date         distribution date (string).
         */
        public RowVM(int recipientId, String recipientName, String donation, int qty, String date) {
            this.recipientId.set(recipientId);
            this.recipientName.set(recipientName);
            this.donation.set(donation);
            this.qty.set(qty);
            this.date.set(date);
        }

        /** @return property for recipient ID. */
        public IntegerProperty recipientIdProperty() { return recipientId; }
        /** @return property for recipient name. */
        public StringProperty  recipientNameProperty() { return recipientName; }
        /** @return property for donation label. */
        public StringProperty  donationProperty() { return donation; }
        /** @return property for quantity. */
        public IntegerProperty qtyProperty() { return qty; }
        /** @return property for date. */
        public StringProperty  dateProperty() { return date; }
    }

    /**
     * FXML handler that refreshes distributions when the donation selection changes.
     * Item ID is optional and left as-is.
     */
    @FXML
    public void onDonationChanged() {
        // Refresh immediately when donation changes (Item ID is optional now)
        viewDistributions(null);
    }
}
