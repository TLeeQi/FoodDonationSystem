package com.example.fooddonationsystem;

import java.sql.*;
import java.util.ArrayList;

import javafx.scene.control.Alert;

/**
 * Database management utility for the Food Donation System.
 * <p>
 * Provides static methods to:
 * <ul>
 *   <li>Connect to the MySQL database.</li>
 *   <li>Initialize application data (donations, recipients, items).</li>
 *   <li>Perform CRUD operations on donations, items, and recipients.</li>
 *   <li>Handle stock updates and distribution assignments.</li>
 * </ul>
 * <p>
 * Note: This class mixes persistence logic with UI alerts
 * ({@link #showError(String, String)}),
 * which could be separated in a layered architecture.
 */

public class dbManagement {
    static PreparedStatement preparedStatement;
    static Statement statement;
    static ResultSet result;
    static Connection con;

    /** Preloaded donation instance: Emergency Food Aid. */
    public static Donation emergencyFoodAid;

    /** Preloaded donation instance: Community Centre Collection. */
    public static Donation communityCentreCollection;

    /** Cached list of all recipients from the database. */
    public static ArrayList<Recipient> recipientList;

    /**
     * Constructor that initializes the DB connection and preloads data.
     */
    public dbManagement(){
        System.out.println(initializeDB());
        initializedInformation();
    }

    /**
     * Establishes a connection to the MySQL database.
     *
     * @return connection status message ("connected"/"not connected").
     */
    public String initializeDB(){
        String output = "";

        String dbURL = "jdbc:mysql://localhost:3306/food_donation_db";
        String username = "root";
        String password = "";
        try {
            con = DriverManager.getConnection(dbURL, username, password);
            if (con != null) {
                output = "Database --> connected";
            }
        }catch (SQLException ex){
            output = "Database --> not connected";
            ex.printStackTrace();
        }
        return output;
    }

    /**
     * Initializes cached objects (donations, items, recipients) from the DB.
     */
    public static void initializedInformation(){
        ArrayList<Item> Beverage = getItemList("beverage");
        ArrayList<Item> Fruit =  getItemList("fruit");

        int BeverageStock = getCategoryStock("beverage");
        int FruitStock = getCategoryStock("fruit");

        emergencyFoodAid = new EmergencyFoodAid(
                "1",
                BeverageStock,
                FruitStock,
                Beverage,
                Fruit
        );
        communityCentreCollection = new CommunityCentreCollection(
                "2",
                BeverageStock,
                FruitStock,
                Beverage,
                Fruit
        );

        System.out.println("\nEmergency Food Aid");
        System.out.println(emergencyFoodAid.toString());

        System.out.println("\nCommunity Centre Collection");
        System.out.println(communityCentreCollection.toString());

        System.out.println("\nRecipients");
        recipientList = getRecipientList();
        System.out.println(recipientList);
    }

    // ===================== Recipients =====================

    /**
     * Retrieves all recipients from the database.
     *
     * @return list of {@link Recipient} objects with details from DB.
     */
    public static ArrayList<Recipient> getRecipientList(){
        ArrayList<Recipient> recipients = new ArrayList<Recipient>();
        try{
            String sql ="SELECT * FROM recipient";
            preparedStatement = con.prepareStatement(sql);
            result = preparedStatement.executeQuery();
            while (result.next()) {
                String id = result.getString("id");
                String email = result.getString("email");
                String gender = result.getString("gender");
                String name = result.getString("name");
                String address = result.getString("address");
                String phone = result.getString("phoneNumber");
                String emergency = result.getString("emergencyContact");
                Recipient recipient = new Recipient(id,name, gender,address, phone,emergency);
                recipients.add(recipient);
            }
        }catch(SQLException e){
            System.out.println(e.toString());
        }
        return recipients;
    }

    /**
     * Retrieves recipients with optional name filter.
     *
     * @param nameLike filter (may be null for all).
     * @param unusedType ignored (legacy param).
     * @return list of recipient rows [id, name, phone].
     */

    // ----- Recipients for right table (NO recipient_type dependency) -----
    public static java.util.List<Object[]> getRecipientsRaw(String nameLike, String unusedType) {
        java.util.List<Object[]> rows = new java.util.ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT r.id, r.name, COALESCE(r.phoneNumber,'') AS phone FROM recipient r WHERE 1=1"
        );
        java.util.List<Object> params = new java.util.ArrayList<>();

        if (nameLike != null && !nameLike.trim().isEmpty()) {
            sql.append(" AND r.name LIKE ?");
            params.add("%" + nameLike.trim() + "%");
        }
        sql.append(" ORDER BY r.id");

        try (PreparedStatement ps = con.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setString(i+1, String.valueOf(params.get(i)));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new Object[]{
                            rs.getInt("id"),       // <- INT recipient id
                            rs.getString("name"),
                            rs.getString("phone")
                    });
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return rows;
    }

    /**
     * Retrieves the list of donation items distributed to a specific recipient.
     * <p>
     * The result contains rows with the following columns:
     * <ul>
     *   <li><b>item_id</b> – the unique identifier of the item (int)</li>
     *   <li><b>item_name</b> – the name of the item (String, never {@code null}, empty if missing)</li>
     *   <li><b>donation_name</b> – the name of the donation/event (String, never {@code null}, empty if missing)</li>
     *   <li><b>quantity</b> – the quantity of this item distributed (int)</li>
     *   <li><b>distribution_date</b> – the distribution date (String, formatted using {@link java.sql.Date#toString()})</li>
     * </ul>
     * The rows are ordered by distribution date (descending) and then by item ID (ascending).
     *
     * @param recipientId the identifier of the recipient whose distribution records should be retrieved
     * @return a list of {@code Object[]} arrays, where each array corresponds to a row of:
     *         {item_id, item_name, donation_name, quantity, distribution_date}
     */
    public static java.util.List<Object[]> getRecipientItems(int recipientId) {
        java.util.List<Object[]> rows = new java.util.ArrayList<>();

        String sql =
                "SELECT dd.item_id, COALESCE(di.item_name,'' ) AS item_name, " +
                        "       COALESCE(d.donation_name,'') AS donation_name, " +
                        "       dd.quantity, dd.distribution_date " +
                        "FROM donation_distribution dd " +
                        "LEFT JOIN donation_item di ON di.item_id = dd.item_id " +
                        "LEFT JOIN donation d       ON d.donation_id = dd.donation_id " +
                        "WHERE dd.recipient_id = ? " +
                        "ORDER BY dd.distribution_date DESC, dd.item_id ASC";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, recipientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new Object[]{
                            rs.getInt("item_id"),
                            rs.getString("item_name"),
                            rs.getString("donation_name"),
                            rs.getInt("quantity"),
                            String.valueOf(rs.getDate("distribution_date")) // or rs.getString(...)
                    });
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return rows;
    }

    /**
     * Find the recipient id exist or not
     * @param id - Recipient ID
     * @return Either exist this recipient or not
     */
    public static boolean findID(int id) {
        try (PreparedStatement ps = con.prepareStatement("SELECT 1 FROM recipient WHERE id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ===================== Items =====================

    /**
     * Retrieves all items for a given donation and category.
     *
     * @param categoryName category filter ("Beverage"/"Fruit").
     * @return list of {@link Item} objects.
     *
     */

    public static ArrayList<Item> getItemList(String categoryName) {
        ArrayList<Item> itemList = new ArrayList<>();
        try {
            String sql = "SELECT di.item_id, di.item_name, di.stock " +
                    "FROM donation_item di " +
                    "JOIN food_category fc ON di.category_id = fc.category_id " +
                    "WHERE fc.category_name = ? " +
                    "ORDER BY di.item_id";
            preparedStatement = con.prepareStatement(sql);
            preparedStatement.setString(1, categoryName);
            result = preparedStatement.executeQuery();
            while (result.next()) {
                int itemID = result.getInt("item_id");
                String itemName = result.getString("item_name");
                int itemStock = result.getInt("stock");

                Item item;

                if (categoryName.equalsIgnoreCase("Beverage")) {
                    item = new Beverage(itemID);
                } else {
                    item = new Fruit(itemID);
                }

                item.setName(itemName);
                item.setStock(itemStock);

                itemList.add(item);
            }
        } catch (SQLException e) {
            System.out.println(e.toString());
        }
        return itemList;
    }

    /**
     * Gets current stock for a specific item.
     *
     * @param itemID item identifier.
     * @return stock count (0 if not found).
     */

    public static int getItemStock(String itemID){
        int stock = 0;
        try{
            String sql ="SELECT stock FROM donation_item WHERE item_id = ?";
            preparedStatement = con.prepareStatement(sql);
            preparedStatement.setString(1, itemID);
            result = preparedStatement.executeQuery();
            while (result.next()) {
                stock = result.getInt("stock");
            }
        }catch(SQLException e){
            System.out.println(e.toString());
        }
        return stock;
    }

    /**
     * Gets stock total for a given category across all donations.
     *
     * @param categoryName category name ("Beverage"/"Fruit").
     * @return total stock sum.
     */

    public static int getCategoryStock(String categoryName) {
        int stock = 0;
        try {
            String sql = "SELECT SUM(di.stock) AS total_stock " +
                    "FROM donation_item di " +
                    "JOIN food_category fc ON di.category_id = fc.category_id " +
                    "WHERE fc.category_name = ?";
            preparedStatement = con.prepareStatement(sql);
            preparedStatement.setString(1, categoryName);
            result = preparedStatement.executeQuery();
            if (result.next()) {
                stock = result.getInt("total_stock");
            }
        } catch (SQLException e) {
            System.out.println("Error fetching stock for category " + categoryName + ": " + e);
        }
        return stock;
    }

    /**
     * Retrieves all donation items (optionally filtered).
     *
     * @param nameLike item name/id filter.
     * @param categoryId category ID (nullable).
     * @return rows [item_id, name, category, stock].
     */

    public static java.util.List<Object[]> getDonationItemsRaw(String nameLike, Integer categoryId) {
        java.util.List<Object[]> rows = new java.util.ArrayList<>();

        StringBuilder sb = new StringBuilder(
                "SELECT di.item_id, di.item_name, fc.category_name, di.stock " +
                        "FROM donation_item di LEFT JOIN food_category fc ON di.category_id = fc.category_id WHERE 1=1"
        );
        java.util.List<Object> params = new java.util.ArrayList<>();

        if (nameLike != null && !nameLike.trim().isEmpty()) {
            sb.append(" AND (di.item_name LIKE ? OR di.item_id LIKE ?)");
            params.add("%" + nameLike.trim() + "%");
            params.add("%" + nameLike.trim() + "%");
        }
        if (categoryId != null) {
            sb.append(" AND di.category_id = ?");
            params.add(categoryId);
        }
        sb.append(" ORDER BY di.item_id");

        try (PreparedStatement ps = con.prepareStatement(sb.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof String)  ps.setString(i+1, (String)p);
                else if (p instanceof Integer) ps.setInt(i+1, (Integer)p);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new Object[]{
                            rs.getInt("item_id"),
                            rs.getString("item_name"),
                            rs.getString("category_name"),
                            rs.getInt("stock")
                    });
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return rows;
    }


    /**
     * @param itemName
     * @param categoryId
     * @param stock
     * @return true if item added successfully
     */
    public static boolean addDonationItem(String itemName, int categoryId, int stock) {
        if (itemName == null || itemName.isBlank()) {
            showError("Error","Item Name is required");
            return false;
        }
        // Optional: prevent duplicate name within same category
        if (findItemByName(itemName, categoryId)) {
            showError("Error","An item with this name already exists in the selected category");
            return false;
        }

        String sql = "INSERT INTO donation_item (item_name, category_id, stock) VALUES (?,?,?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, itemName);
            ps.setInt(2, categoryId);
            ps.setInt(3, stock);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * @param itemId
     * @return status
     */
    // Delete by INT item_id with safety check against distributed items
    public static String deleteDonationItemById(int itemId) {
        // has it been distributed?
        int distributed = getDistributedCount(itemId);
        if (distributed > 0) {
            showError("Error", "This item has already been distributed; cannot delete. Set stock to 0 instead.");
            return "Item cannot be deleted because it has distributions. Set stock=0 instead.";
        }

        String sql = "DELETE FROM donation_item WHERE item_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            int rows = ps.executeUpdate();
            return rows > 0 ? "Item deleted successfully." : "No item found for that ID.";
        } catch (SQLException e) {
            e.printStackTrace();
            return "Failed to delete the item. Please try again.";
        }
    }

    /**
     * Updates the stock level of a donation item.
     *
     * @param itemId   the unique identifier of the item
     * @param newStock the new stock quantity to set
     * @return {@code true} if exactly one row was updated, {@code false} otherwise
     */
    public static boolean updateItemStock(int itemId, int newStock) {
        String sql = "UPDATE donation_item SET stock = ? WHERE item_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, newStock);
            ps.setInt(2, itemId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Reduces the stock of a donation item by the specified quantity.
     * <p>
     * If the result would go below zero, the stock is capped at zero.
     *
     * @param itemID   the unique identifier of the item
     * @param quantity the quantity to subtract from the stock
     */
    public static void reduceItemStock(int itemID, int quantity) {
        String selectSQL = "SELECT stock FROM donation_item WHERE item_id = ?";
        try (PreparedStatement ps = con.prepareStatement(selectSQL)) {
            ps.setInt(1, itemID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int current = rs.getInt("stock");
                    int next = Math.max(current - quantity, 0);
                    try (PreparedStatement up = con.prepareStatement(
                            "UPDATE donation_item SET stock = ? WHERE item_id = ?")) {
                        up.setInt(1, next);
                        up.setInt(2, itemID);
                        up.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Restores (increases) the stock of a donation item by the specified quantity.
     *
     * @param itemID   the unique identifier of the item
     * @param quantity the quantity to add back to the stock
     */
    public static void restoreItemStock(int itemID, int quantity) {
        String selectSQL = "SELECT stock FROM donation_item WHERE item_id = ?";
        try (PreparedStatement ps = con.prepareStatement(selectSQL)) {
            ps.setInt(1, itemID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int current = rs.getInt("stock");
                    int next = current + quantity;
                    try (PreparedStatement up = con.prepareStatement(
                            "UPDATE donation_item SET stock = ? WHERE item_id = ?")) {
                        up.setInt(1, next);
                        up.setInt(2, itemID);
                        up.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Checks if an item exists in the database by its ID.
     *
     * @param id the unique identifier of the item
     * @return {@code true} if the item exists, {@code false} otherwise
     */
    public static boolean findItemID(int id) {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT 1 FROM donation_item WHERE item_id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Checks if an item exists by its name and category.
     *
     * @param itemName   the name of the item
     * @param categoryId the category identifier the item belongs to
     * @return {@code true} if an item with the given name exists in the category,
     *         {@code false} otherwise
     */
    public static boolean findItemByName(String itemName, int categoryId) {
        String sql = "SELECT 1 FROM donation_item WHERE item_name = ? AND category_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, itemName);
            ps.setInt(2, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Retrieves the category ID for a given category name.
     * <p>
     * The lookup is case-insensitive.
     *
     * @param name the category name
     * @return the category ID if found, otherwise {@code null}
     */
    public static Integer getCategoryIdByName(String name) {
        String sql = "SELECT category_id FROM food_category WHERE LOWER(category_name)=LOWER(?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // ===================== Distributions =====================
    /**
     * Assigns a donation item to a recipient with a specific quantity.
     * <p>
     * Performs the following validations before assignment:
     * <ul>
     *     <li>Recipient exists</li>
     *     <li>Donation item exists</li>
     *     <li>Item has enough stock</li>
     *     <li>Donation record exists</li>
     * </ul>
     * If successful, inserts a new row into {@code donation_distribution} and reduces stock accordingly.
     *
     * @param itemID      the donation item ID
     * @param recipientID the recipient ID (integer foreign key from {@code recipient})
     * @param quantity    the quantity of the item to assign
     * @param donationId  the donation record ID
     * @return a status message indicating success or failure
     */

    // Assign item to recipient (NO recipient_type column; recipient is INT)
    public static String addRecipientToDonation(int itemID, int recipientID, int quantity, int donationId) {
        // basic validations
        if (!findID(recipientID)) {
            showError("Error", "Recipient not found");
            return "Recipient not found";
        }
        if (!findItemID(itemID)) {
            showError("Error", "Donation item not found");
            return "Donation item not found";
        }
        if (!checkItemAvailability(itemID, quantity)) {
            showError("Error", "Not enough stock for this donation item");
            return "Not enough stock";
        }
        if (!findDonationId(donationId)) {
            showError("Error", "Donation not found");
            return "Donation not found";
        }

        String sql = "INSERT INTO donation_distribution " +
                "(item_id, recipient_id, donation_id, quantity, distribution_date) " +
                "VALUES (?,?,?,?, CURRENT_DATE)";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, itemID);
            ps.setInt(2, recipientID);   // <- INT
            ps.setInt(3, donationId);
            ps.setInt(4, quantity);

            int rows = ps.executeUpdate();
            if (rows > 0) {
                reduceItemStock(itemID, quantity);
                return "Recipient successfully assigned to donation item";
            } else {
                return "Failed to assign recipient to donation item";
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "Fail to assign recipient to donation item, please try again";
        }
    }

    /**
     * Removes a recipient's assignment for a specific donation item.
     * <p>
     * If the recipient-item pair exists, deletes the record from
     * {@code donation_distribution} and restores the quantity back to stock.
     *
     * @param itemID      the donation item ID
     * @param recipientID the recipient ID
     * @return a message indicating whether the removal was successful
     */
    public static String deleteRecipientFromDonation(int itemID, int recipientID) {
        try {
            // Ensure exists
            String selectSQL = "SELECT quantity FROM donation_distribution WHERE item_id = ? AND recipient_id = ?";
            try (PreparedStatement ps = con.prepareStatement(selectSQL)) {
                ps.setInt(1, itemID);
                ps.setInt(2, recipientID);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        showError("Error", "This recipient has not been assigned this donation item");
                        return "Recipient was not assigned to this item.";
                    }
                    int qty = rs.getInt("quantity");

                    // Delete row
                    String deleteSQL = "DELETE FROM donation_distribution WHERE item_id = ? AND recipient_id = ?";
                    try (PreparedStatement del = con.prepareStatement(deleteSQL)) {
                        del.setInt(1, itemID);
                        del.setInt(2, recipientID);
                        int rows = del.executeUpdate();
                        if (rows > 0) {
                            // restore stock
                            restoreItemStock(itemID, qty);
                            return "Recipient successfully removed from donation item (success).";
                        } else {
                            return "Failed to remove recipient from donation item.";
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "Fail to delete the recipient from donation item, please try again.";
        }
    }

    /**
     * Retrieves all raw distribution records, optionally filtered by donation ID.
     * <p>
     * Each row contains:
     * <ul>
     *     <li>Item ID</li>
     *     <li>Item name</li>
     *     <li>Recipient ID</li>
     *     <li>Recipient name</li>
     *     <li>Quantity distributed</li>
     *     <li>Distribution date</li>
     *     <li>Donation ID</li>
     * </ul>
     *
     * @param donationId if provided, filters distributions by donation ID; if {@code null}, retrieves all
     * @return a list of object arrays representing distribution records
     */
    // Returns current assignments; filter by donationId if provided (nullable)
    public static java.util.List<Object[]> getDistributionsRaw(Integer donationId) {
        java.util.List<Object[]> rows = new java.util.ArrayList<>();
        StringBuilder sb = new StringBuilder(
                "SELECT dd.item_id, di.item_name, dd.recipient_id, r.name AS recipient_name, " +
                        "       dd.quantity, dd.distribution_date, dd.donation_id " +
                        "FROM donation_distribution dd " +
                        "LEFT JOIN donation_item di ON dd.item_id = di.item_id " +
                        "LEFT JOIN recipient r ON dd.recipient_id = r.id " +
                        "WHERE 1=1 "
        );
        if (donationId != null) sb.append(" AND dd.donation_id = ? ");
        sb.append("ORDER BY dd.distribution_date DESC, dd.item_id");

        try (PreparedStatement ps = con.prepareStatement(sb.toString())) {
            if (donationId != null) ps.setInt(1, donationId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new Object[] {
                            rs.getInt("item_id"),
                            rs.getString("item_name"),
                            rs.getInt("recipient_id"),
                            rs.getString("recipient_name"),
                            rs.getInt("quantity"),
                            rs.getDate("distribution_date"),
                            rs.getInt("donation_id")
                    });
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return rows;
    }

    /**
     * Retrieves detailed distribution records for a specific item, optionally filtered by donation ID.
     * <p>
     * Each row contains:
     * <ul>
     *     <li>Recipient ID</li>
     *     <li>Recipient name</li>
     *     <li>Donation name</li>
     *     <li>Quantity distributed</li>
     *     <li>Distribution date (formatted as YYYY-MM-DD)</li>
     * </ul>
     *
     * @param itemId     optional item ID to filter by (nullable)
     * @param donationId optional donation ID to filter by (nullable)
     * @return a list of object arrays representing item-level distributions
     */
    public static java.util.List<Object[]> getItemDistributions(Integer itemId, Integer donationId) {
        java.util.List<Object[]> rows = new java.util.ArrayList<>();

        StringBuilder sql = new StringBuilder(
                "SELECT dd.recipient_id, r.name AS recipient_name, d.donation_name, " +
                        "       dd.quantity, DATE_FORMAT(dd.distribution_date, '%Y-%m-%d') AS distribution_date " +
                        "FROM donation_distribution dd " +
                        "JOIN recipient r ON r.id = dd.recipient_id " +
                        "LEFT JOIN donation d ON d.donation_id = dd.donation_id " +
                        "WHERE 1=1 "
        );

        java.util.List<Object> params = new java.util.ArrayList<>();

        if (itemId != null) {
            sql.append(" AND dd.item_id = ?");
            params.add(itemId);
        }
        if (donationId != null) {
            sql.append(" AND dd.donation_id = ?");
            params.add(donationId);
        }

        sql.append(" ORDER BY dd.distribution_date DESC, dd.recipient_id");

        try (PreparedStatement ps = con.prepareStatement(sql.toString())) {
            int idx = 1;
            for (Object p : params) {
                if (p instanceof Integer) ps.setInt(idx++, (Integer) p);
                else ps.setString(idx++, String.valueOf(p));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new Object[] {
                            rs.getInt("recipient_id"),
                            rs.getString("recipient_name"),
                            rs.getString("donation_name"),
                            rs.getInt("quantity"),
                            rs.getString("distribution_date")
                    });
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }

        return rows;
    }

    // ===================== Donations =====================

    /**
     * Retrieves all donations (id + name).
     *
     * @return rows [donation_id, donation_name].
     */

    public static java.util.List<Object[]> getDonationsRaw() {
        java.util.List<Object[]> rows = new java.util.ArrayList<>();
        String sql = "SELECT donation_id, donation_name FROM donation ORDER BY donation_id";
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new Object[]{ rs.getInt("donation_id"), rs.getString("donation_name") });
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return rows;
    }

    /**
     * Checks if a donation with the given ID exists in the database.
     *
     * @param donationId the donation ID to look up
     * @return {@code true} if the donation exists, {@code false} otherwise
     */
    public static boolean findDonationId(int donationId) {
        String sql = "SELECT 1 FROM donation WHERE donation_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, donationId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // ===================== UI Helpers =====================

    /**
     * Displays an error alert dialog.
     *
     * @param title title of dialog.
     * @param msg   error message.
     */

    /**
     * Show the error message
     * @param title - Error message title
     * @param msg - Error message content
     */
    public static void showError(String title, String msg){
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    /**
     * Retrieves all available donation items (with stock > 0) for display.
     *
     * @param donationID the donation ID (currently unused in the query, but may be used for filtering in the future)
     * @return a formatted string listing available item IDs, or an error message if the query fails
     */
    public static String showAvailableDonationItems(int donationID) {
        StringBuilder output = new StringBuilder();
        try {
            String sql = "SELECT item_id FROM donation_item WHERE stock > 0 ORDER BY item_id";
            preparedStatement = con.prepareStatement(sql);
            result = preparedStatement.executeQuery();
            while (result.next()) {
                String itemID = result.getString("item_id");
                output.append("|").append(itemID).append("|  ");
            }
        } catch (SQLException e) {
            return "Fail to find the donation items";
        }
        return output.toString();
    }

    /**
     * Gets the total quantity of a specific item that has already been distributed.
     *
     * @param itemId the ID of the donation item
     * @return the total distributed quantity for that item; 0 if none or on error
     */
    private static int getDistributedCount(int itemId) {
        String sql = "SELECT COALESCE(SUM(quantity),0) AS total FROM donation_distribution WHERE item_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("total");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Checks whether a given donation item has sufficient stock available.
     *
     * @param itemID            the ID of the donation item
     * @param requestedQuantity the quantity being requested
     * @return {@code true} if stock is available and sufficient, {@code false} otherwise
     */
    public static boolean checkItemAvailability(int itemID, int requestedQuantity) {
        String sql = "SELECT stock FROM donation_item WHERE item_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, itemID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int stockQuantity = rs.getInt("stock");
                    return stockQuantity >= requestedQuantity;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}