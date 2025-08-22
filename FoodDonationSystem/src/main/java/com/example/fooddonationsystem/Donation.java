package com.example.fooddonationsystem;

import java.util.ArrayList;

/**
 * Abstract domain model representing a donation drive/event.
 * <p>
 * A {@code Donation} aggregates two item categories (Beverage, Fruit), keeps
 * a snapshot of total stock per category, and holds lists of the items that
 * belong to each category. Subclasses (e.g., {@code EmergencyFoodAid},
 * {@code CommunityCentreCollection}) specialize the type/name semantics.
 * <p>
 * Notes:
 * <ul>
 *   <li>Totals are snapshots; recompute or set via setters when stock changes.</li>
 *   <li>Item lists are references; update the lists when items change.</li>
 * </ul>
 */
public abstract class Donation {

    // --- Identity/metadata (mapped to `donation` table semantics) ---
    /** Donation identifier (e.g., "1"). */
    private String donationId;
    /** Optional display name of the donation (may be {@code null}). */
    private String donationName;
    /** Donation type label (e.g., "Community Centre Collection", "Emergency Food Aid"). */
    private String donationType;
    /** Optional location descriptor (may be {@code null}). */
    private String donationLocation;

    // --- Category stock snapshot (not per-item; totals computed externally) ---
    /** Snapshot of total beverage stock across items for this donation. */
    private int totalBeverageStock;
    /** Snapshot of total fruit stock across items for this donation. */
    private int totalFruitStock;

    // --- Item lists grouped by category (populated via DAO methods) ---
    /** Items categorized as beverage. */
    private ArrayList<Item> beverageItems;
    /** Items categorized as fruit. */
    private ArrayList<Item> fruitItems;

    /**
     * No-arg constructor creating a sentinel donation with "unknown" identity and empty lists.
     * <p>
     * Equivalent to calling:
     * {@code ("unknown", null, null, null, 0, 0, new ArrayList<>(), new ArrayList<>())}
     */
    protected Donation() {
        this("unknown", null, null, null, 0, 0,
                new ArrayList<>(), new ArrayList<>());
    }

    /**
     * Convenience constructor matching common call sites, without name/type/location.
     *
     * @param donationId         donation identifier as string (kept as String for compatibility)
     * @param totalBeverageStock snapshot of total beverage stock
     * @param totalFruitStock    snapshot of total fruit stock
     * @param beverageItems      list of beverage items (non-null preferred)
     * @param fruitItems         list of fruit items (non-null preferred)
     */
    protected Donation(String donationId,
                       int totalBeverageStock,
                       int totalFruitStock,
                       ArrayList<Item> beverageItems,
                       ArrayList<Item> fruitItems) {
        this(donationId, null, null, null,
                totalBeverageStock, totalFruitStock,
                beverageItems, fruitItems);
    }

    /**
     * Full constructor allowing name/type/location metadata.
     *
     * @param donationId         donation identifier as string
     * @param donationName       human-friendly donation name (nullable)
     * @param donationType       donation type label (nullable)
     * @param donationLocation   optional location (nullable)
     * @param totalBeverageStock snapshot of total beverage stock
     * @param totalFruitStock    snapshot of total fruit stock
     * @param beverageItems      list of beverage items (if {@code null}, an empty list is created)
     * @param fruitItems         list of fruit items (if {@code null}, an empty list is created)
     */
    protected Donation(String donationId,
                       String donationName,
                       String donationType,
                       String donationLocation,
                       int totalBeverageStock,
                       int totalFruitStock,
                       ArrayList<Item> beverageItems,
                       ArrayList<Item> fruitItems) {
        this.donationId = donationId;
        this.donationName = donationName;
        this.donationType = donationType;
        this.donationLocation = donationLocation;
        this.totalBeverageStock = totalBeverageStock;
        this.totalFruitStock = totalFruitStock;
        this.beverageItems = (beverageItems != null) ? beverageItems : new ArrayList<>();
        this.fruitItems = (fruitItems != null) ? fruitItems : new ArrayList<>();
    }

    // -------- Getters / Setters --------

    /**
     * Returns the donation identifier.
     * @return the donation identifier (string)
     */
    public String getDonationId() { return donationId; }

    /**
     * Returns the optional display name for this donation.
     * @return the donation display name, or {@code null} if unset
     */
    public String getDonationName() { return donationName; }

    /**
     * Returns the donation type label (e.g., "Emergency Food Aid").
     * @return the donation type label, or {@code null} if unset
     */
    public String getDonationType() { return "Donation"; }

    /**
     * Returns the donation location label.
     * @return the donation location, or {@code null} if unset
     */
    public String getDonationLocation() { return donationLocation; }

    /**
     * Returns the current snapshot of total beverage stock.
     * @return current snapshot of total beverage stock
     */
    public int getTotalBeverageStock() { return totalBeverageStock; }

    /**
     * Returns the current snapshot of total fruit stock.
     * @return current snapshot of total fruit stock
     */
    public int getTotalFruitStock() { return totalFruitStock; }

    /**
     * Returns the list of beverage items for this donation.
     * @return mutable list of beverage items (never {@code null}); modifying it affects this instance
     */
    public ArrayList<Item> getBeverageItems() { return beverageItems; }

    /**
     * Returns the list of fruit items for this donation.
     * @return mutable list of fruit items (never {@code null}); modifying it affects this instance
     */
    public ArrayList<Item> getFruitItems() { return fruitItems; }

    /**
     * Updates the beverage stock snapshot (recompute externally, then set).
     * @param totalBeverageStock new snapshot value
     */
    public void setTotalBeverageStock(int totalBeverageStock) { this.totalBeverageStock = totalBeverageStock; }

    /**
     * Updates the fruit stock snapshot (recompute externally, then set).
     * @param totalFruitStock new snapshot value
     */
    public void setTotalFruitStock(int totalFruitStock) { this.totalFruitStock = totalFruitStock; }

    // -------- Convenience renderers (legacy-compatible) --------

    /**
     * Builds a compact string of beverage item IDs in the format {@code |<id>|  } repeated.
     * @return concatenated beverage item IDs for display
     */
    public String getBeverage() {
        StringBuilder sb = new StringBuilder();
        for (Item it : beverageItems) sb.append("|").append(it.getItemId()).append("|  ").append(it.getName()).append("|  ");
        return sb.toString();
    }

    /**
     * Builds a compact string of fruit item IDs in the format {@code |<id>|  } repeated.
     * @return concatenated fruit item IDs for display
     */
    public String getFruit() {
        StringBuilder sb = new StringBuilder();
        for (Item it : fruitItems) sb.append("|").append(it.getItemId()).append("|  ").append(it.getName()).append("|  ");
        return sb.toString();
    }

    /**
     * Returns a combined string representation used by legacy UI code,
     * consisting of {@link #getBeverage()} followed by {@link #getFruit()}.
     *
     * @return concatenated beverage and fruit ID strings
     */
    @Override
    public String toString() {
        return getDonationType() + getBeverage() + getFruit();
    }
}
