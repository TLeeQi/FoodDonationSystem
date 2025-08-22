package com.example.fooddonationsystem;

/**
 * Domain model representing a donation recipient (person).
 * <p>
 * Encapsulates identity and contact details for a recipient. Some methods
 * from an older design and currently map to the same underlying {@code id}
 * field to represent an assigned item identifier. Consider refactoring those
 * to clearer names (e.g., {@code getAssignedItemId()}, {@code setAssignedItemId(...)})
 * in a future pass.
 */
public class Recipient {
    /** Recipient identifier. Also (legacy) repurposed to hold an assigned item id via {@link #setItem(String)}. */
    private String id;
    /** Recipient full name. */
    private String name;
    /** Postal address. */
    private String address;
    /** Gender string (free-form). */
    private String gender;
    /** Contact phone number. */
    private String phoneNumber;
    /** Emergency contact phone or details. */
    private String emergencyContact;

    /**
     * No-arg constructor creating a placeholder recipient with default fields.
     * Scope is {@code protected} to encourage construction via factories/builders.
     */
    protected Recipient() {
        this("-", "-", "-", "-", "-",
                "-");
    }

    /**
     * Full constructor for a recipient.
     *
     * @param id               recipient ID.
     * @param name             recipient name.
     * @param gender           recipient gender.
     * @param address          recipient address.
     * @param phoneNumber      recipient phone number.
     * @param emergencyContact emergency contact information.
     */
    protected Recipient(String id, String name, String gender, String address, String phoneNumber, String emergencyContact) {
        this.name = name;
        this.gender = gender;
        this.address = address;
        this.phoneNumber = phoneNumber;
        this.emergencyContact = emergencyContact;
        this.id = id;
    }

    /**
     * @return the recipient ID.
     */
    public String getId() {
        return id;
    }

    /**
     * @return recipient name.
     */
    public String getName() { return name; }

    /**
     * Sets the recipient name.
     *
     * @param name new name value.
     */
    public void setName(String name) { this.name = name; }

    /**
     * @return recipient gender.
     */
    public String getGender() {
        return gender;
    }

    /**
     * Sets the recipient gender.
     *
     * @param gender new gender value.
     */
    public void setGender(String gender) {
        this.gender = gender;
    }

    /**
     * @return recipient address.
     */
    public String getAddress() { return address; }

    /**
     * Sets the recipient address.
     *
     * @param address new address value.
     */
    public void setAddress(String address) { this.address = address; }

    /**
     * @return recipient phone number.
     */
    public String getPhoneNumber() { return phoneNumber; }

    /**
     * Sets the recipient phone number.
     *
     * @param phoneNumber new phone value.
     */
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    /**
     * @return recipient emergency contact details.
     */
    public String getEmergencyContact() {
        return emergencyContact;
    }

    /**
     * Sets the recipient emergency contact details.
     *
     * @param emergencyContact new emergency contact value.
     */
    public void setEmergencyContact(String emergencyContact) {
        this.emergencyContact = emergencyContact;
    }

    /**
     * Returns a multi-line string representation of the recipient.
     *
     * @return formatted string with ID, name, address, gender, phone, and emergency contact.
     */
    @Override
    public String toString() {
        return "\n\nID: " + id +
                "\nName: " + name +
                "\nAddress: " + address +
                "\nGender: " + gender +
                "\nPhone Number: " + phoneNumber +
                "\nEmergency Contact: " + emergencyContact;
    }
}
