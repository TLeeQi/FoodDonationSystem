package com.example.fooddonationsystem;

/**
 * Policy for capping how many units a recipient can receive in one assignment.
 * The policy itself does NOT hit the database; it only uses inputs you pass in.
 */
public interface AllocationPolicy {
    /**
     * @return maximum units this recipient can receive PER ASSIGNMENT, ignoring stock.
     */
    int maxPerAssignment(int itemId, int recipientId);
}
