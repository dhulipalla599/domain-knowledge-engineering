package com.dke.healthcare.patient_registration_and_master_patient_index.domain;

public class DuplicatePatientException extends RuntimeException {
    private final String existingMrn;
    private final int score;

    public DuplicatePatientException(String existingMrn, int score) {
        super("Registration matches existing record %s (score %d)".formatted(existingMrn, score));
        this.existingMrn = existingMrn;
        this.score = score;
    }

    public String getExistingMrn() { return existingMrn; }
    public int getScore() { return score; }
}
