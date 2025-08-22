package com.example.fooddonationsystem;

/**
 * Concrete subclass of {@link Item} representing a Beverage category item.
 * <p>
 * Demonstrates inheritance (extends {@link Item}) and polymorphism
 * by overriding {@link #toString()} and providing a category-specific
 * {@link #getCategoryName()} implementation.
 */
public class Beverage extends Item {

    /**
     * Default constructor.
     * Creates a Beverage with a placeholder item ID ("-").
     */
    public Beverage() {
        super(0);
    }

    /**
     * Constructs a Beverage with the specified item ID.
     *
     * @param itemId unique identifier for the beverage item.
     */
    public Beverage(int itemId) {
        super(itemId);
    }

    /**
     * Returns the category name for this item.
     * <p>
     * Overridden from {@link Item} to always return "Beverage".
     *
     * @return the string "Beverage".
     */
    public String getCategoryName() {
        return "Beverage";
    }

    /**
     * Provides a formatted string representation of the Beverage item.
     * <p>
     * Includes both the item ID (inherited from {@link Item})
     * and its category name.
     *
     * @return string representation of the Beverage item.
     */
    @Override
    public String toString() {
        return "Item ID: " + getItemId() + " | Category: " + getCategoryName();
    }
}
