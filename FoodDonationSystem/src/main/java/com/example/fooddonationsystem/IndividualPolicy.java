package com.example.fooddonationsystem;

/** Simple policy: individuals can receive up to 5 units per assignment. */
public class IndividualPolicy implements AllocationPolicy {
    @Override
    public int maxPerAssignment(int itemId, int recipientId) {
        return 5;
    }
}
