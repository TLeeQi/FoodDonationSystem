package com.example.fooddonationsystem;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;

/**
 * Controller for viewing and updating donation item stock.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>List items with optional text and category filters.</li>
 *   <li>Allow selection or manual entry of an Item ID.</li>
 *   <li>Validate and submit stock updates via {@link dbManagement#updateItemStock(int, int)}.</li>
 *   <li>Provide inline success/error feedback.</li>
 * </ul>
 */
public class UpdateStockController {

    // ===== Table =====

    /** @FXML TableView injected from FXML. Displays items eligible for stock updates. */
    @FXML private TableView<ItemVM> itemsTable;

    /** @FXML TableColumn injected from FXML. Item ID column. */
    @FXML private TableColumn<ItemVM, Number> colId;

    /** @FXML TableColumn injected from FXML. Item name column. */
    @FXML private TableColumn<ItemVM, String> colName;

    /** @FXML TableColumn injected from FXML. Current stock column. */
    @FXML private TableColumn<ItemVM, Number> colStock;

    // ===== Filters =====

    /**
     * @FXML ComboBox injected from FXML.
     * Category filter with values: "All Categories", "Beverage", "Fruit".
     */
    @FXML private ComboBox<String> categoryFilter;

    /**
     * @FXML TextField injected from FXML.
     * Optional text filter matched against item ID or name.
     */
    @FXML private TextField itemSearch;

    // ===== Form =====

    /**
     * @FXML TextField injected from FXML.
     * Item ID to update. Auto-filled when a table row is selected; digits-only formatter applied.
     */
    @FXML private TextField itemIdField;

    /**
     * @FXML TextField injected from FXML.
     * New stock value (non-negative integer). Digits-only formatter applied.
     */
    @FXML private TextField itemStockField;

    /**
     * @FXML Label injected from FXML.
     * Displays status feedback (green for success, red for error).
     */
    @FXML private Label statusLabel;

    /** Backing list bound to {@link #itemsTable}. */
    private final ObservableList<ItemVM> rows = FXCollections.observableArrayList();

    /**
     * Initializes table columns, input formatters, default filters, and loads initial data.
     * Also wires table selection to populate the Item ID field.
     */
    public void initialize() {
        // Table wiring
        colId.setCellValueFactory(d -> d.getValue().idProperty());
        colName.setCellValueFactory(d -> d.getValue().nameProperty());
        colStock.setCellValueFactory(d -> d.getValue().stockProperty());
        itemsTable.setItems(rows);

        // Click row -> fill Item ID
        itemsTable.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null && itemIdField != null) itemIdField.setText(String.valueOf(n.getId()));
        });

        // Numeric-only inputs
        if (itemIdField != null) {
            itemIdField.setTextFormatter(new TextFormatter<>(c ->
                    c.getControlNewText().matches("\\d{0,9}") ? c : null));
        }
        if (itemStockField != null) {
            itemStockField.setTextFormatter(new TextFormatter<>(c ->
                    c.getControlNewText().matches("\\d{0,9}") ? c : null));
        }

        // Category filter
        if (categoryFilter != null) {
            categoryFilter.getItems().setAll("All Categories", "Beverage", "Fruit");
            categoryFilter.setValue("All Categories");
        }

        refresh();
    }

    /**
     * Reloads the items table using the current filters (category and optional text).
     * Pulls data from {@link dbManagement#getDonationItemsRaw(String, Integer)}.
     */
    private void refresh() {
        rows.clear();

        Integer catId = selectedCategoryId();
        String nameLike = null;
        if (itemSearch != null && itemSearch.getText() != null && !itemSearch.getText().isBlank()) {
            nameLike = itemSearch.getText().trim();
        }

        for (Object[] r : dbManagement.getDonationItemsRaw(nameLike, catId)) {
            int    id    = (r[0] instanceof Number) ? ((Number) r[0]).intValue() : parseIntSafe(r[0]);
            String name  = r[1] == null ? "" : String.valueOf(r[1]);
            int    stock = (r[3] instanceof Number) ? ((Number) r[3]).intValue() : 0;
            rows.add(new ItemVM(id, name, stock));
        }
    }

    /**
     * FXML handler for category filter changes.
     * Triggers a table refresh.
     */
    @FXML
    public void onCategoryFilter() { refresh(); }

    /**
     * FXML handler for text search changes (e.g., on key events or a search button).
     * Triggers a table refresh.
     */
    @FXML
    public void onItemSearch() { refresh(); }

    /**
     * FXML handler to update an item's stock.
     * <p>
     * Flow:
     * <ol>
     *   <li>Validate Item ID (integer).</li>
     *   <li>Validate stock (integer, non-negative).</li>
     *   <li>Verify the item exists via {@link dbManagement#findItemID(int)}.</li>
     *   <li>Call {@link dbManagement#updateItemStock(int, int)} and show feedback.</li>
     *   <li>Refresh the table on success.</li>
     * </ol>
     */
    @FXML
    public void updateStock() {
        setStatus("", false);

        // Validate item id
        final int itemId;
        try {
            itemId = Integer.parseInt(itemIdField.getText().trim());
        } catch (Exception e) {
            setStatus("Please enter a valid Item ID (integer).", true);
            return;
        }

        // Validate stock
        final int newStock;
        try {
            newStock = Integer.parseInt(itemStockField.getText().trim());
            if (newStock < 0) {
                setStatus("Stock cannot be negative.", true);
                return;
            }
        } catch (Exception e) {
            setStatus("Please enter a valid stock (integer).", true);
            return;
        }

        // Check item exists
        if (!dbManagement.findItemID(itemId)) {
            setStatus("Item not found (ID: " + itemId + ").", true);
            return;
        }

        // Update
        boolean ok = dbManagement.updateItemStock(itemId, newStock);
        if (ok) {
            setStatus("Stock updated.", false);
            refresh();
            if (itemStockField != null) itemStockField.clear();
        } else {
            setStatus("Failed to update stock. Please try again.", true);
        }
    }

    /**
     * FXML handler to clear the form fields and table selection.
     */
    @FXML
    public void clear() {
        if (itemIdField != null) itemIdField.clear();
        if (itemStockField != null) itemStockField.clear();
        setStatus("", false);
        if (itemsTable != null) itemsTable.getSelectionModel().clearSelection();
    }

    /**
     * Updates the status label with the provided message and color (red for errors, green for success).
     *
     * @param msg   message to display.
     * @param error whether the message indicates an error.
     */
    private void setStatus(String msg, boolean error) {
        if (statusLabel != null) {
            statusLabel.setTextFill(error ? Color.RED : Color.GREEN);
            statusLabel.setText(msg);
        }
    }

    /**
     * Parses an integer from a generic object, returning 0 on failure.
     *
     * @param o object to parse.
     * @return parsed int or 0 if not parsable.
     */
    private int parseIntSafe(Object o) {
        try { return Integer.parseInt(String.valueOf(o)); } catch (Exception e) { return 0; }
    }

    /**
     * Maps the selected category label to its database category ID.
     *
     * @return category ID for "Beverage"/"Fruit", or {@code null} for "All Categories" or unset.
     */
    private Integer selectedCategoryId() {
        if (categoryFilter == null) return null;
        String sel = categoryFilter.getValue();
        if (sel == null || sel.equalsIgnoreCase("All Categories")) return null;
        if (sel.equalsIgnoreCase("Beverage")) return dbManagement.getCategoryIdByName("Beverage");
        if (sel.equalsIgnoreCase("Fruit"))    return dbManagement.getCategoryIdByName("Fruit");
        return null;
    }

    // ===== Table View-Model =====

    /**
     * Minimal view-model for the items table. Wraps fields in JavaFX properties for binding.
     */
    public static class ItemVM {
        private final IntegerProperty id = new SimpleIntegerProperty();
        private final StringProperty  name = new SimpleStringProperty();
        private final IntegerProperty stock = new SimpleIntegerProperty();

        /**
         * Constructs a row view-model.
         *
         * @param id    item identifier.
         * @param name  item display name.
         * @param stock current stock value.
         */
        public ItemVM(int id, String name, int stock) { this.id.set(id); this.name.set(name); this.stock.set(stock); }

        /** @return property for item ID. */
        public IntegerProperty idProperty() { return id; }
        /** @return property for item name. */
        public StringProperty  nameProperty() { return name; }
        /** @return property for stock. */
        public IntegerProperty stockProperty() { return stock; }

        /** @return primitive item ID (helper for listeners). */
        public int getId() { return id.get(); }
    }
}
