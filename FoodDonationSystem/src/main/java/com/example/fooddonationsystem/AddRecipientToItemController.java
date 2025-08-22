package com.example.fooddonationsystem;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;

/**
 * Controller for assigning recipients to donation items.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Load and filter items/recipients.</li>
 *   <li>Bind table selections to the bottom form.</li>
 *   <li>Validate input and call {@code dbManagement} to link a recipient to a donation item.</li>
 * </ul>
 */
public class AddRecipientToItemController {

    /** Default constructor required by FXMLLoader (documented to satisfy doclint). */
    public AddRecipientToItemController() { }

    // ===== Top controls =====

    /**
     * ComboBox injected from FXML.
     * Holds donation selections in format "ID - Name" (e.g., "1 - Emergency Food Aid").
     */
    @FXML private ComboBox<String> donationPicker;

    /**
     * ComboBox injected from FXML.
     * Category filter for items: "All Categories", "Beverage", "Fruit".
     */
    @FXML private ComboBox<String> categoryFilter;

    /**
     * Text field injected from FXML.
     * Live text filter for recipient names.
     */
    @FXML private TextField recipientSearch;

    // ===== Items table =====

    /** TableView injected from FXML. Displays available items. */
    @FXML private TableView<ItemVM> itemsTable;

    /** TableColumn injected from FXML. Shows item ID (numeric). */
    @FXML private TableColumn<ItemVM, Number> colItemId;

    /** TableColumn injected from FXML. Shows item name. */
    @FXML private TableColumn<ItemVM, String> colItemName;

    /** TableColumn injected from FXML. Shows current stock. */
    @FXML private TableColumn<ItemVM, Number> colItemStock;

    @FXML private Label policyHintLabel;


    // ===== Recipients table =====

    /** TableView injected from FXML. Lists potential recipients. */
    @FXML private TableView<RecipientVM> recipientsTable;

    /** Recipient ID column (kept as String for display). */
    @FXML private TableColumn<RecipientVM, String> colRecId;

    /** Recipient name column. */
    @FXML private TableColumn<RecipientVM, String> colRecName;

    /** Recipient phone column. */
    @FXML private TableColumn<RecipientVM, String> colRecPhone;

    // ===== Bottom form =====

    /**
     * Optional manual input for Item ID (auto-filled when selecting a row).
     */
    @FXML private TextField itemNumber;

    /**
     * Optional manual input for Recipient ID (auto-filled when selecting a row).
     */
    @FXML private TextField recipientID;

    /**
     * Quantity of items to assign to the recipient.
     */
    @FXML private TextField quantityField;

    /**
     * UI-only recipient type (not persisted in DB). Values: "IndividualPolicy", "Organisation".
     */
    @FXML private ComboBox<String> recipientTypeCombo;

    /**
     * Status message area for success/error feedback.
     */
    @FXML private Label statusLabel;

    /** Backing list for the items table. */
    private final ObservableList<ItemVM> itemRows = FXCollections.observableArrayList();

    /** Backing list for the recipients table. */
    private final ObservableList<RecipientVM> recipientRows = FXCollections.observableArrayList();

    /**
     * Initializes UI bindings, loads initial data, and wires listeners.
     * <ul>
     *   <li>Populates donation picker and category filter.</li>
     *   <li>Configures numeric constraint on {@code recipientID}.</li>
     *   <li>Sets table cell value factories and row-click behaviors.</li>
     *   <li>Enables live recipient search.</li>
     *   <li>Loads initial items and recipients.</li>
     * </ul>
     */
    @FXML
    public void initialize() {
        // Donation picker
        if (donationPicker != null) {
            for (Object[] d : dbManagement.getDonationsRaw()) {
                donationPicker.getItems().add(d[0] + " - " + String.valueOf(d[1]));
            }
            if (!donationPicker.getItems().isEmpty()) {
                donationPicker.setValue(donationPicker.getItems().get(0));
            }
        }

        // Category filter
        if (categoryFilter != null) {
            categoryFilter.getItems().setAll("All Categories", "Beverage", "Fruit");
            categoryFilter.setValue("All Categories");
        }

        // Recipient ID numeric guard
        if (recipientID != null) {
            recipientID.setPromptText("e.g. 1");
            recipientID.setTextFormatter(new TextFormatter<>(c ->
                    c.getControlNewText().matches("\\d{0,9}") ? c : null));
        }

        // Recipient Type (UI-only)
        if (recipientTypeCombo != null) {
            recipientTypeCombo.getItems().setAll("IndividualPolicy", "Organisation");
            recipientTypeCombo.setValue("IndividualPolicy");
        }

        // Items table
        if (itemsTable != null) {
            colItemId.setCellValueFactory(d -> d.getValue().idProperty());
            colItemName.setCellValueFactory(d -> d.getValue().nameProperty());
            colItemStock.setCellValueFactory(d -> d.getValue().stockProperty());
            itemsTable.setItems(itemRows);

            // Clicking a row -> populate Item ID field
            itemsTable.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
                if (n != null && itemNumber != null) itemNumber.setText(String.valueOf(n.getId()));
            });
        }

        // Recipients table
        if (recipientsTable != null) {
            colRecId.setCellValueFactory(d -> d.getValue().idProperty());
            colRecName.setCellValueFactory(d -> d.getValue().nameProperty());
            colRecPhone.setCellValueFactory(d -> d.getValue().phoneProperty());
            recipientsTable.setItems(recipientRows);

            // Clicking a row -> populate Recipient ID field
            recipientsTable.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
                if (n != null && recipientID != null) recipientID.setText(n.getId());
            });
        }

        // Live recipient name search
        if (recipientSearch != null) {
            recipientSearch.textProperty().addListener((obs, o, n) -> refreshRecipients());
        }

        // Update hint when selection/inputs change
        if (recipientTypeCombo != null) {
            recipientTypeCombo.valueProperty().addListener((o,old,v)-> refreshPolicyHint());
        }
        if (itemNumber != null) {
            itemNumber.textProperty().addListener((o,old,v)-> refreshPolicyHint());
        }
        if (recipientID != null) {
            recipientID.textProperty().addListener((o,old,v)-> refreshPolicyHint());
        }
        // Also refresh after item table selection (since we auto-fill itemNumber)
        if (itemsTable != null) {
            itemsTable.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> refreshPolicyHint());
        }
        // After initial loads
        refreshPolicyHint();

        // Initial loads
        refreshItems();
        refreshRecipients();
    }

    /**
     * FXML handler for category filter change (ComboBox action).
     * Refreshes the items list with the current filter.
     */
    @FXML
    public void onCategoryFilter() { refreshItems(); }

    /**
     * Resolves the selected donation ID from the donation picker.
     * @return the donation ID, or {@code null} if none/invalid.
     */
    private Integer selectedDonationId() {
        if (donationPicker == null) return null;
        String s = donationPicker.getValue();
        if (s == null || s.isBlank()) return null;
        int dash = s.indexOf(" - ");
        if (dash <= 0) return null;
        try { return Integer.parseInt(s.substring(0, dash).trim()); }
        catch (NumberFormatException e) { return null; }
    }

    /**
     * Resolves the category ID matching the current category filter.
     * @return the DB category ID, or {@code null} for "All".
     */
    private Integer selectedCategoryId() {
        if (categoryFilter == null) return null;
        String sel = categoryFilter.getValue();
        if (sel == null || sel.equalsIgnoreCase("All Categories")) return null;
        if (sel.equalsIgnoreCase("Beverage")) return dbManagement.getCategoryIdByName("Beverage");
        if (sel.equalsIgnoreCase("Fruit"))    return dbManagement.getCategoryIdByName("Fruit");
        return null;
    }

    /**
     * Reloads items from the database applying the current category filter.
     * Populates {@link #itemRows}.
     */
    private void refreshItems() {
        itemRows.clear();
        Integer catId = selectedCategoryId();
        for (Object[] r : dbManagement.getDonationItemsRaw(null, catId)) {
            // r[0]=item_id (int), r[1]=item_name (String), r[2]=category_name (String), r[3]=stock (int)
            int    id    = (r[0] instanceof Number) ? ((Number) r[0]).intValue() : parseIntSafe(r[0]);
            String name  = r[1] == null ? "" : String.valueOf(r[1]);
            int    stock = (r[3] instanceof Number) ? ((Number) r[3]).intValue() : 0;
            itemRows.add(new ItemVM(id, name, stock));
        }
    }

    /**
     * Reloads recipients from the database applying the current name filter.
     * Populates {@link #recipientRows}.
     */
    private void refreshRecipients() {
        recipientRows.clear();
        String nameLike = (recipientSearch == null) ? null : recipientSearch.getText();
        for (Object[] r : dbManagement.getRecipientsRaw(nameLike, null)) {
            // r[0]=id (INT in DB, but display as String), r[1]=name, r[2]=phone
            String id    = r[0] == null ? "" : String.valueOf(r[0]);
            String name  = r[1] == null ? "" : String.valueOf(r[1]);
            String phone = r[2] == null ? "" : String.valueOf(r[2]);
            recipientRows.add(new RecipientVM(id, name, phone));
        }
    }

    /**
     * Assigns the selected/entered recipient to the selected/entered item
     * for the currently selected donation, decreasing stock accordingly.
     *
     * @param e the action event from the "Add" button.
     */
    @FXML
    public void addRecipientToDonation(ActionEvent e) {
        setStatus("", false);

        // Item ID
        final int itemId;
        try {
            itemId = Integer.parseInt(itemNumber.getText().trim());
        } catch (Exception ex) {
            setStatus("Please enter/select a valid Item ID (integer).", true);
            return;
        }

        // Recipient ID
        final int recipientId;
        try {
            recipientId = Integer.parseInt(recipientID.getText().trim());
        } catch (Exception ex) {
            setStatus("Please enter/select a valid Recipient ID (integer).", true);
            return;
        }

        // Quantity
        final int qty;
        try {
            qty = Integer.parseInt(quantityField.getText().trim());
            if (qty <= 0) {
                setStatus("Quantity must be > 0.", true);
                return;
            }
        } catch (Exception ex) {
            setStatus("Please enter a valid quantity (integer).", true);
            return;
        }

        // Donation
        Integer donationId = selectedDonationId();
        if (donationId == null) {
            setStatus("Please select a Donation.", true);
            return;
        }

        if (recipientTypeCombo != null) {
            recipientTypeCombo.valueProperty().addListener((o, old, val) -> updatePolicyHint());
        }
        if (itemNumber != null) {
            itemNumber.textProperty().addListener((o, old, val) -> updatePolicyHint());
        }
        // show the initial hint
        updatePolicyHint();

        // Allocation Policy enforcement
        AllocationPolicy policy = selectedPolicy();
        int available = availableStockForItem(itemId);
        int policyCap = policy.maxPerAssignment(itemId, recipientId);
        int limit = Math.min(policyCap, available);
        // Enforce allocation policy
        String type = (recipientTypeCombo == null) ? null : recipientTypeCombo.getValue();
        int policyLimit = computePolicyLimit(type, itemId);
        if (policyLimit != Integer.MAX_VALUE && qty > policyLimit) {
            setStatus("Requested quantity (" + qty + ") exceeds policy limit (" + policyLimit + ") for " + type + ".", true);
            return;
        }

        if (qty > limit) {
            setStatus("Requested quantity " + qty + " exceeds limit " + limit +
                    " (available " + available + ", policy cap " + policyCap + ").", true);
            return;
        }

        // DB call
        String result = dbManagement.addRecipientToDonation(itemId, recipientId, qty, donationId);

        boolean ok = result.toLowerCase().contains("success");
        setStatus(result, !ok);
        if (ok) {
            quantityField.clear();
            refreshItems(); // reflect decreased stock
            refreshPolicyHint();
        }
    }

    /**
     * Clears the form and selection state.
     *
     * @param e the action event from the "Clear" button.
     */
    @FXML
    public void clear(ActionEvent e) {
        if (itemNumber != null) itemNumber.clear();
        if (recipientID != null) recipientID.clear();
        if (quantityField != null) quantityField.clear();
        if (recipientTypeCombo != null) recipientTypeCombo.setValue("IndividualPolicy"); // UI only
        setStatus("", false);
        if (itemsTable != null) itemsTable.getSelectionModel().clearSelection();
        if (recipientsTable != null) recipientsTable.getSelectionModel().clearSelection();
    }

    /**
     * Sets the status label text and color.
     *
     * @param msg   message to display.
     * @param error {@code true} to display in red; {@code false} for green.
     */
    private void setStatus(String msg, boolean error) {
        if (statusLabel != null) {
            statusLabel.setTextFill(error ? Color.RED : Color.GREEN);
            statusLabel.setText(msg);
        }
    }

    /**
     * Parses an integer safely from an unknown object.
     *
     * @param o source object.
     * @return parsed integer, or 0 if parsing fails.
     */
    private int parseIntSafe(Object o) {
        try { return Integer.parseInt(String.valueOf(o)); } catch (Exception e) { return 0; }
    }

    // ====== View Models ======

    /**
     * View-model for items displayed in {@link #itemsTable}.
     * Uses JavaFX properties for easy binding.
     */
    public static class ItemVM {
        private final IntegerProperty id = new SimpleIntegerProperty();
        private final StringProperty  name = new SimpleStringProperty();
        private final IntegerProperty stock = new SimpleIntegerProperty();

        /**
         * Constructs an item view-model.
         *
         * @param id    item ID.
         * @param name  item name.
         * @param stock current stock.
         */
        public ItemVM(int id, String name, int stock) {
            this.id.set(id); this.name.set(name); this.stock.set(stock);
        }

        /**
         * Returns the ID property used for table binding.
         * @return ID property.
         */
        public IntegerProperty idProperty() { return id; }

        /**
         * Returns the Name property used for table binding.
         * @return Name property.
         */
        public StringProperty  nameProperty() { return name; }

        /**
         * Returns the Stock property used for table binding.
         * @return Stock property.
         */
        public IntegerProperty stockProperty() { return stock; }

        /**
         * Returns the primitive ID value (convenient for listeners).
         * @return the item ID value.
         */
        public int getId() { return id.get(); }
    }

    /** Returns the policy chosen in the UI. */
    private AllocationPolicy selectedPolicy() {
        String v = (recipientTypeCombo == null || recipientTypeCombo.getValue() == null)
                ? "IndividualPolicy" : recipientTypeCombo.getValue();
        return "Organisation".equalsIgnoreCase(v) ? new OrganisationPolicy() : new IndividualPolicy();
    }

    /** Returns current available stock for itemId, preferring the on-screen table snapshot. */
    private int availableStockForItem(int itemId) {
        // Try from the table’s backing list first (avoids DB round-trip).
        for (ItemVM vm : itemRows) {
            if (vm.getId() == itemId) return vm.stockProperty().get();
        }
        // Fallback to DB if the item isn’t in the current view.
        return dbManagement.getItemStock(String.valueOf(itemId));
    }

    // TEMP policy: tweak these numbers or replace with your AllocationPolicy classes later.
    private int computePolicyLimit(String recipientType, int itemId) {
        if (recipientType == null) return Integer.MAX_VALUE; // no limit if unknown
        if ("Organisation".equalsIgnoreCase(recipientType)) return 10;
        if ("IndividualPolicy".equalsIgnoreCase(recipientType)) return 5;
        return Integer.MAX_VALUE; // default = no limit
    }

    private void updatePolicyHint() {
        if (policyHintLabel == null) return;
        int itemId = 0;
        try { itemId = Integer.parseInt(itemNumber.getText().trim()); } catch (Exception ignore) {}
        String type = (recipientTypeCombo == null) ? null : recipientTypeCombo.getValue();
        int limit = computePolicyLimit(type, itemId);
        policyHintLabel.setText(limit == Integer.MAX_VALUE
                ? "No per-assignment limit for this selection."
                : "Max per-assignment for " + type + ": " + limit);
    }


    /** Recomputes the hint text under Quantity (cap = min(policy cap, available stock)). */
    private void refreshPolicyHint() {
        if (policyHintLabel == null) return;
        try {
            int itemId = Integer.parseInt(itemNumber.getText().trim());
            int recId  = Integer.parseInt(recipientID.getText().trim());
            int available = availableStockForItem(itemId);
            int policyCap = selectedPolicy().maxPerAssignment(itemId, recId);
            int limit = Math.min(policyCap, available);

            policyHintLabel.setText("Max allowed: " + limit + "  (Available: " + available + ", Policy cap: " + policyCap + ")");
        } catch (Exception ignore) {
            policyHintLabel.setText("");
        }
    }



    /**
     * View-model for recipients displayed in {@link #recipientsTable}.
     * Uses JavaFX properties for easy binding.
     */
    public static class RecipientVM {
        private final StringProperty id = new SimpleStringProperty();
        private final StringProperty name = new SimpleStringProperty();
        private final StringProperty phone = new SimpleStringProperty();

        /**
         * Constructs a recipient view-model.
         *
         * @param id    recipient ID (displayed as String).
         * @param name  recipient full name.
         * @param phone recipient phone number.
         */
        public RecipientVM(String id, String name, String phone) {
            this.id.set(id); this.name.set(name); this.phone.set(phone);
        }

        /**
         * Returns the ID property used for table binding.
         * @return ID property.
         */
        public StringProperty idProperty() { return id; }

        /**
         * Returns the Name property used for table binding.
         * @return Name property.
         */
        public StringProperty nameProperty() { return name; }

        /**
         * Returns the Phone property used for table binding.
         * @return Phone property.
         */
        public StringProperty phoneProperty() { return phone; }

        /**
         * Returns the ID as a plain string (useful for text fields).
         * @return the ID string.
         */
        public String getId() { return id.get(); }
    }
}
