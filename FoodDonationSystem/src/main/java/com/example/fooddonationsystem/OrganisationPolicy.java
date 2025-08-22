package com.example.fooddonationsystem;

/** Simple policy: organisations can receive up to 20 units per assignment. */
public class OrganisationPolicy implements AllocationPolicy {
    @Override
    public int maxPerAssignment(int itemId, int recipientId) {
        return 20;
    }
}
