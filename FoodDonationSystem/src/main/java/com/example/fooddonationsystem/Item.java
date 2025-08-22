package com.example.fooddonationsystem;

/**
 * Abstract base class for donation items.
 * <p>
 * Encapsulates the common identity field {@code itemId}. Concrete subclasses
 * such as {@link Beverage} and {@link Fruit} specialize category semantics and
 * presentation (e.g., adding a {@code getCategoryName()} method).
 * <p>
 * Design notes:
 * <ul>
 *   <li>This class is abstract to signal it represents a general concept.</li>
 *   <li>Subclasses typically provide category-specific behavior and formatting.</li>
 * </ul>
 */
public abstract class Item {

    /** Unique identifier for the item (stringified for UI/compatibility). */
    private int itemId;
    private String name;   // optional, but handy
    private int stock;     // <- snapshot value loaded from DB

    /**
     * No-arg constructor creating a placeholder item with a hyphen ID.
     * <p>
     * Useful for default initialization paths and testing.
     */
    protected Item() {
        this(0);
    }

    /**
     * Constructs an item with the provided identifier.
     *
     * @param itemId item identifier (non-null preferred).
     */
    protected Item(int itemId) {
        this.itemId = itemId;
    }

    /**
     * Returns the item identifier.
     *
     * @return item ID string.
     */
    public int getItemId() {
        return itemId;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    /** Current stock snapshot (authoritative value is in DB). */
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    /**
     * Returns a basic string representation including the item ID.
     *
     * @return formatted string with the item ID.
     */
    @Override
    public String toString() {
        return "Item ID: " + itemId;
    }
}
