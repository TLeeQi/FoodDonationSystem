package com.example.fooddonationsystem;

/**
 * Concrete subclass of {@link Item} representing a Fruit category item.
 * <p>
 * Demonstrates inheritance (extends {@link Item}) and polymorphism
 * by specializing {@link #getCategoryName()} and formatting {@link #toString()}.
 */
public class Fruit extends Item {

    /**
     * Default constructor.
     * Creates a Fruit with a placeholder item ID ("-").
     */
    public Fruit() {
        super(0);
    }

    /**
     * Constructs a Fruit with the specified item ID.
     *
     * @param itemId unique identifier for the fruit item.
     */
    public Fruit(int itemId) {
        super(itemId);
    }

    /**
     * Returns the category name for this item.
     * <p>
     * Specialization for fruits; always returns {@code "Fruit"}.
     *
     * @return the string "Fruit".
     */
    public String getCategoryName() {
        return "Fruit";
    }

    /**
     * Provides a formatted string representation of the Fruit item.
     * <p>
     * Includes both the item ID (inherited from {@link Item})
     * and its category name.
     *
     * @return string representation of the Fruit item.
     */
    @Override
    public String toString() {
        return "Item ID: " + getItemId() + " | Category: " + getCategoryName();
    }
}
