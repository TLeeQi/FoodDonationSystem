package com.example.fooddonationsystem;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TextFormatter;
import java.util.function.UnaryOperator;

/**
 * Controller for the “Add Item” screen in the Donation Management System.
 * <p>
 * Handles adding new donation items (Beverage or Fruit), listing them in a table,
 * and filtering by category. Database operations are delegated to {@code dbManagement}.
 */
public class AddItemController {

    /** Default constructor required by FXMLLoader (added to satisfy doclint). */
    public AddItemController() { }

    /** Dropdown for filtering items by category (All, Beverage, Fruit). */
    @FXML private ComboBox<String> donation;

    /** Legacy text area for displaying items (kept for backward compatibility). */
    @FXML private TextArea item;

    /** Radio button for Beverage category selection. */
    @FXML private RadioButton beverage;

    /** Radio button for Fruit category selection. */
    @FXML private RadioButton fruit;

    /** Text field for entering the item name. */
    @FXML private TextField itemNameField;

    /** Text field for entering stock quantity (fx:id=itemNumber kept for compatibility). */
    @FXML private TextField itemNumber;

    /** Label used to display error/success messages. */
    @FXML private Label wrongAddItem;

    /** Table view for displaying existing items. */
    @FXML private TableView<ItemVM> itemsTable;

    /** Table column for Item ID. */
    @FXML private TableColumn<ItemVM, String> colId;

    /** Table column for Item Name. */
    @FXML private TableColumn<ItemVM, String> colName;

    /** Table column for Item Category. */
    @FXML private TableColumn<ItemVM, String> colCategory;

    /** Table column for Item Stock. */
    @FXML private TableColumn<ItemVM, Number> colStock;

    /** Backing list for {@link #itemsTable}. */
    private final ObservableList<ItemVM> items = FXCollections.observableArrayList();

    /**
     * Initializes UI bindings and loads initial data after FXML injection.
     * <ul>
     *   <li>Sets up the category filter dropdown.</li>
     *   <li>Groups category radio buttons.</li>
     *   <li>Applies a digits-only formatter to the stock field.</li>
     *   <li>Wires table columns to {@link ItemVM} properties.</li>
     *   <li>Loads the initial item list.</li>
     * </ul>
     */
    @FXML
    public void initialize() {
        // Category filter
        donation.getItems().setAll("All Categories", "Beverage", "Fruit");
        donation.setOnAction(e -> displayItem());

        // Radio group
        ToggleGroup catGroup = new ToggleGroup();
        beverage.setToggleGroup(catGroup);
        fruit.setToggleGroup(catGroup);

        // Digits-only for stock
        UnaryOperator<TextFormatter.Change> digitsOnly =
                c -> c.getControlNewText().matches("\\d{0,9}") ? c : null;
        itemNumber.setTextFormatter(new TextFormatter<>(digitsOnly));

        // Table wiring
        if (itemsTable != null) {
            colId.setCellValueFactory(data -> data.getValue().idProperty());
            colName.setCellValueFactory(data -> data.getValue().nameProperty());
            colCategory.setCellValueFactory(data -> data.getValue().categoryProperty());
            colStock.setCellValueFactory(data -> data.getValue().stockProperty());
            itemsTable.setItems(items);
        }

        // Initial load (no filter)
        refreshItemsTable(null);
    }

    /**
     * Filters the table by the selected category in {@link #donation}
     * and updates the legacy {@link #item} text area.
     */
    @FXML
    public void displayItem() {
        String sel = donation.getValue();
        Integer categoryId = categoryIdFromName(sel); // null for "All"

        // Update table
        refreshItemsTable(categoryId);

        // Legacy text area (optional)
        if (item == null) return;
        if ("Beverage".equalsIgnoreCase(sel)) {
            item.setText("-------------------------------------------Beverage Items-----------------------------------------\n"
                    + dbManagement.showAvailableDonationItems(0));
        } else if ("Fruit".equalsIgnoreCase(sel)) {
            item.setText("-------------------------------------------Fruit Items--------------------------------------------\n"
                    + dbManagement.showAvailableDonationItems(0));
        } else {
            item.setText("No available items.");
        }
    }

    /**
     * Validates input, inserts a new donation item, and refreshes the table.
     *
     * @param event action event from the “Add” button.
     */
    @FXML
    public void addItem(ActionEvent event) {
        wrongAddItem.setTextFill(Color.RED);

        // 1) Item name
        String itemName = (itemNameField == null || itemNameField.getText() == null)
                ? "" : itemNameField.getText().trim().replaceAll("\\s{2,}", " ");
        if (itemName.isEmpty()) {
            wrongAddItem.setText("Please enter Item Name.");
            return;
        }

        // 2) Stock
        String stockText = (itemNumber == null || itemNumber.getText() == null)
                ? "" : itemNumber.getText().trim();
        if (stockText.isEmpty()) {
            wrongAddItem.setText("Please enter stock (whole number).");
            return;
        }
        int stock;
        try {
            stock = Integer.parseInt(stockText);
            if (stock < 0) {
                wrongAddItem.setText("Stock cannot be negative.");
                return;
            }
        } catch (NumberFormatException nfe) {
            wrongAddItem.setText("Stock must be a whole number (e.g., 0, 10, 100).");
            return;
        }

        // 3) Category
        Integer cat = dbManagement.getCategoryIdByName(beverage.isSelected() ? "Beverage" : "Fruit");
        if (cat == null) {
            wrongAddItem.setText("Category not found in database.");
            return;
        }

        // 4) Insert
        boolean dbOk = dbManagement.addDonationItem(itemName, cat, stock);

        if (dbOk) {
            wrongAddItem.setTextFill(Color.GREEN);
            wrongAddItem.setText("Added successfully.");

            // Refresh with current filter
            Integer filterCat = categoryIdFromName(donation.getValue());
            refreshItemsTable(filterCat);

            // Clear inputs
            if (itemNameField != null) itemNameField.clear();
            if (itemNumber != null) itemNumber.clear();
        } else {
            wrongAddItem.setText("DB insert failed. (Check connection/table/duplicate name.)");
        }
    }

    /**
     * Clears the form and resets the category selection to Beverage.
     *
     * @param event action event from the “Clear” button.
     */
    @FXML
    public void clear(ActionEvent event) {
        beverage.setSelected(true);
        fruit.setSelected(false);
        itemNameField.clear();
        itemNumber.clear();
        wrongAddItem.setText("");
    }

    // ===== Helpers =====

    /**
     * Maps a category name to its database ID.
     *
     * @param sel category label selected in the dropdown.
     * @return {@code 1} for Beverage, {@code 2} for Fruit, or {@code null} for “All Categories”/unknown.
     */
    private Integer categoryIdFromName(String sel) {
        if (sel == null || sel.equalsIgnoreCase("All Categories")) return null;
        if (sel.equalsIgnoreCase("Beverage")) return 1;
        if (sel.equalsIgnoreCase("Fruit")) return 2;
        return null;
    }

    /**
     * Loads items from the database into the table, optionally filtered by category.
     *
     * @param categoryId category filter, or {@code null} for all items.
     */
    private void refreshItemsTable(Integer categoryId) {
        if (itemsTable == null) return;
        items.clear();

        for (Object[] r : dbManagement.getDonationItemsRaw(null, categoryId)) {
            String id       = r[0] == null ? "" : String.valueOf(r[0]);
            String name     = (r[1] instanceof String && !((String) r[1]).isEmpty()) ? (String) r[1] : id;
            String category = r[2] == null ? "" : (String) r[2];
            int stock       = (r[3] instanceof Number) ? ((Number) r[3]).intValue() : 0;

            items.add(new ItemVM(id, name, category, stock));
        }
    }

    /**
     * View model representing a donation item for the table.
     * Wraps fields in JavaFX {@link javafx.beans.property.Property} types for binding.
     */
    public static class ItemVM {
        private final StringProperty  id = new SimpleStringProperty();
        private final StringProperty  name = new SimpleStringProperty();
        private final StringProperty  category = new SimpleStringProperty();
        private final IntegerProperty stock = new SimpleIntegerProperty();

        /**
         * Constructs a new {@code ItemVM}.
         *
         * @param id       item identifier (string for display).
         * @param name     item name (falls back to ID when empty).
         * @param category category label.
         * @param stock    current stock quantity.
         */
        public ItemVM(String id, String name, String category, int stock) {
            this.id.set(id);
            this.name.set(name);
            this.category.set(category);
            this.stock.set(stock);
        }

        /**
         * Returns the Item ID property used for table binding.
         * @return Item ID property.
         */
        public StringProperty idProperty() { return id; }

        /**
         * Returns the Item Name property used for table binding.
         * @return Item Name property.
         */
        public StringProperty nameProperty() { return name; }

        /**
         * Returns the Category property used for table binding.
         * @return Category property.
         */
        public StringProperty categoryProperty() { return category; }

        /**
         * Returns the Stock property used for table binding.
         * @return Stock property.
         */
        public IntegerProperty stockProperty() { return stock; }
    }
}
