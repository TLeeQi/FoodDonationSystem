package com.example.fooddonationsystem;

import java.util.ArrayList;

/**
 * Concrete subtype of {@link Donation} representing the
 * <em>Emergency Food Aid</em> donation type.
 * <p>
 * Demonstrates inheritance from {@link Donation} and provides a fixed
 * {@code donationType} value that aligns with your database enum.
 */
public class EmergencyFoodAid extends Donation {

    /** Constant type name used in DB and business logic. */
    private static final String TYPE = "Emergency Food Aid";

    /**
     * Constructs a new Emergency Food Aid donation aggregate.
     *
     * @param donationId         unique identifier for this donation.
     * @param totalBeverageStock snapshot of total beverage stock available.
     * @param totalFruitStock    snapshot of total fruit stock available.
     * @param beverageItems      list of beverage {@link Item}s associated with this donation.
     * @param fruitItems         list of fruit {@link Item}s associated with this donation.
     */
    public EmergencyFoodAid(String donationId,
                            int totalBeverageStock,
                            int totalFruitStock,
                            ArrayList<Item> beverageItems,
                            ArrayList<Item> fruitItems) {
        super(donationId, null, TYPE, null,
                totalBeverageStock, totalFruitStock,
                beverageItems, fruitItems);
    }

    @Override public String getDonationType() { return "Emergency Food Aid"; }


    /**
     * Returns a string representation of this donation.
     * <p>
     * Delegates to {@link Donation#toString()}, which prints a compact
     * concatenation of beverage and fruit item IDs for legacy UI display.
     *
     * @return formatted string with donation details.
     */
    @Override
    public String toString() {
        return super.toString();
    }
}
