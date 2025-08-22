package com.example.fooddonationsystem;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Controller for deleting donation items.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Filter and display items in a table (optionally by category).</li>
 *   <li>Allow the user to select or type an Item ID and delete it.</li>
 *   <li>Validate inputs, show confirmation, invoke {@link dbManagement} delete API, and refresh the table.</li>
 * </ul>
 */
public class DeleteItemController {

    /** Default constructor required by FXMLLoader (doclint-friendly). */
    public DeleteItemController() { }

    // ===== Top controls =====

    /**
     * @FXML ComboBox injected from FXML.
     * Acts as a category filter with values: "All Categories", "Beverage", "Fruit".
     */
    @FXML private ComboBox<String> donation;

    /**
     * @FXML TextArea injected from FXML.
     * Legacy/hidden area kept for compatibility (not used in the current UI flow).
     */
    @FXML private TextArea item;

    // ===== Delete form inputs =====

    /**
     * @FXML TextField injected from FXML.
     * Holds the Item ID (integer) to delete. Populated on table row click or manual input.
     */
    @FXML private TextField itemNumber;

    /**
     * @FXML Label injected from FXML.
     * Displays validation errors or success messages for deletion.
     */
    @FXML private Label wrongDelete;

    // ===== Items table =====

    /** @FXML TableView injected from FXML. Displays the current list of items. */
    @FXML private TableView<ItemVM> itemsTable;

    /** @FXML TableColumn injected from FXML. Item ID column (stringified). */
    @FXML private TableColumn<ItemVM, String> colId;

    /** @FXML TableColumn injected from FXML. Item name column. */
    @FXML private TableColumn<ItemVM, String> colName;

    /** @FXML TableColumn injected from FXML. Item category column. */
    @FXML private TableColumn<ItemVM, String> colCategory;

    /** @FXML TableColumn injected from FXML. Item stock column. */
    @FXML private TableColumn<ItemVM, Number> colStock;

    /** Backing list for {@link #itemsTable}. */
    private final ObservableList<ItemVM> items = FXCollections.observableArrayList();

    /**
     * Initializes UI components:
     * <ul>
     *   <li>Populates the category filter dropdown.</li>
     *   <li>Configures table cell factories and binds the backing list.</li>
     *   <li>Loads initial items (no filter).</li>
     *   <li>Wires row selection to fill the Item ID text field.</li>
     * </ul>
     */
    @FXML
    public void initialize() {
        // filter entries
        donation.getItems().setAll("All Categories", "Beverage", "Fruit");

        // table wiring
        colId.setCellValueFactory(d -> d.getValue().idProperty());
        colName.setCellValueFactory(d -> d.getValue().nameProperty());
        colCategory.setCellValueFactory(d -> d.getValue().categoryProperty());
        colStock.setCellValueFactory(d -> d.getValue().stockProperty());
        itemsTable.setItems(items);

        // load all items initially
        refreshItemsTable(null);

        // clicking a row fills the Item ID textbox
        itemsTable.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) itemNumber.setText(n.idProperty().get());
        });
    }

    /**
     * FXML handler to apply the selected category filter and refresh the table.
     * Typically bound to the category {@link #donation} ComboBox action.
     */
    @FXML
    public void displayItem() {
        Integer categoryId = categoryIdFromName(donation.getValue());
        refreshItemsTable(categoryId);
    }

    /**
     * FXML handler to delete an item by ID.
     * <p>
     * Flow:
     * <ol>
     *   <li>Validate that {@link #itemNumber} contains a valid integer.</li>
     *   <li>Ask for user confirmation.</li>
     *   <li>Call {@link dbManagement#deleteDonationItemById(int)}.</li>
     *   <li>Show success/error message and refresh the table with the current filter.</li>
     * </ol>
     *
     * @param e action event from the "Delete" button.
     */
    @FXML
    public void deleteItem(ActionEvent e) {
        wrongDelete.setTextFill(Color.RED);
        String idText = itemNumber.getText() == null ? "" : itemNumber.getText().trim();
        if (idText.isEmpty()) {
            wrongDelete.setText("Please enter Item ID.");
            return;
        }
        int itemId;
        try {
            itemId = Integer.parseInt(idText);
        } catch (NumberFormatException ex) {
            wrongDelete.setText("Item ID must be a number.");
            return;
        }

        // Confirm
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete item ID " + itemId + "? This cannot be undone.",
                ButtonType.OK, ButtonType.CANCEL);
        confirm.setHeaderText("Confirm Deletion");
        confirm.showAndWait();
        if (confirm.getResult() != ButtonType.OK) return;

        String msg = dbManagement.deleteDonationItemById(itemId);
        if (msg.toLowerCase().contains("success")) {
            wrongDelete.setTextFill(Color.GREEN);
        } else {
            wrongDelete.setTextFill(Color.RED);
        }
        wrongDelete.setText(msg);

        // refresh table with current filter
        refreshItemsTable(categoryIdFromName(donation.getValue()));
        // optional: clear field
        itemNumber.clear();
    }

    /**
     * FXML handler to clear form and selection state.
     *
     * @param e action event from the "Clear" button.
     */
    @FXML
    public void clear(ActionEvent e) {
        itemNumber.clear();
        wrongDelete.setText("");
        itemsTable.getSelectionModel().clearSelection();
    }

    // ---- helpers ----

    /**
     * Converts a category display name into a database category ID.
     *
     * @param sel category label from the ComboBox.
     * @return {@code 1} for Beverage, {@code 2} for Fruit, or {@code null} for "All Categories"/unknown.
     */
    private Integer categoryIdFromName(String sel) {
        if (sel == null || sel.equalsIgnoreCase("All Categories")) return null;
        if (sel.equalsIgnoreCase("Beverage")) return 1;
        if (sel.equalsIgnoreCase("Fruit")) return 2;
        return null;
    }

    /**
     * Loads items from the database (optionally filtered by category) and populates the table.
     *
     * @param categoryId category filter ID, or {@code null} for all categories.
     */
    private void refreshItemsTable(Integer categoryId) {
        items.clear();
        for (Object[] r : dbManagement.getDonationItemsRaw(null, categoryId)) {
            String id       = r[0] == null ? "" : String.valueOf(r[0]);
            String name     = (r[1] instanceof String && !((String) r[1]).isEmpty()) ? (String) r[1] : id;
            String category = r[2] == null ? "" : (String) r[2];
            int stock       = (r[3] instanceof Number) ? ((Number) r[3]).intValue() : 0;
            items.add(new ItemVM(id, name, category, stock));
        }
    }

    // ===== Table view-model =====

    /**
     * Lightweight view-model for a row in the items table.
     * Wraps values in JavaFX properties for binding in {@link TableView}.
     */
    public static class ItemVM {
        private final StringProperty  id = new SimpleStringProperty();
        private final StringProperty  name = new SimpleStringProperty();
        private final StringProperty  category = new SimpleStringProperty();
        private final IntegerProperty stock = new SimpleIntegerProperty();

        /**
         * Constructs an item view-model row.
         *
         * @param id       item ID as string (display-safe).
         * @param name     item name (falls back to ID if blank).
         * @param category category display name.
         * @param stock    current stock count.
         */
        public ItemVM(String id, String name, String category, int stock) {
            this.id.set(id); this.name.set(name); this.category.set(category); this.stock.set(stock);
        }

        /** Returns the JavaFX property for the item ID. @return the item ID property. */
        public StringProperty idProperty() { return id; }

        /** Returns the JavaFX property for the item name. @return the item name property. */
        public StringProperty nameProperty() { return name; }

        /** Returns the JavaFX property for the category. @return the category property. */
        public StringProperty categoryProperty() { return category; }

        /** Returns the JavaFX property for the stock count. @return the stock property. */
        public IntegerProperty stockProperty() { return stock; }
    }
}
