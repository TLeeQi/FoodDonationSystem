package com.example.fooddonationsystem;

import java.util.ArrayList;

/**
 * Concrete subtype of {@link Donation} representing a
 * Community Centre Collection donation type.
 * <p>
 * Demonstrates inheritance from {@link Donation} and provides
 * a specific donation type string that matches the database enum.
 * <p>
 * This class is used when a donation is collected and distributed
 * via community centres.
 */
public class CommunityCentreCollection extends Donation {

    /** Constant type name used in DB and business logic. */
    private static final String TYPE = "Community Centre Collection";

    /**
     * Constructs a new Community Centre Collection donation.
     *
     * @param donationId          unique identifier for this donation.
     * @param totalBeverageStock  total stock of beverage items available.
     * @param totalFruitStock     total stock of fruit items available.
     * @param beverageItems       list of beverage {@link Item}s associated with this donation.
     * @param fruitItems          list of fruit {@link Item}s associated with this donation.
     */
    public CommunityCentreCollection(String donationId,
                                     int totalBeverageStock,
                                     int totalFruitStock,
                                     ArrayList<Item> beverageItems,
                                     ArrayList<Item> fruitItems) {
        super(donationId, null, TYPE, null,
                totalBeverageStock, totalFruitStock,
                beverageItems, fruitItems);
    }

    @Override
    public String getDonationType() { return "Community Centre Collection"; }


    /**
     * Returns the string representation of this donation.
     * <p>
     * Currently, it delegates to {@link Donation#toString()},
     * which includes details such as donation ID, type,
     * and stock counts.
     *
     * @return formatted string with donation details.
     */
    @Override
    public String toString() {
        return super.toString();
    }
}
