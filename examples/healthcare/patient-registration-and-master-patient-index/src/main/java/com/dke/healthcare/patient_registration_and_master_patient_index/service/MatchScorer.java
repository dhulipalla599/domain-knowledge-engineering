package com.dke.healthcare.patient_registration_and_master_patient_index.service;

import com.dke.healthcare.patient_registration_and_master_patient_index.domain.Patient;
import java.time.LocalDate;
import java.util.Locale;

/**
 * Deterministic matching rules (a stand-in for a probabilistic/ML matcher). Score is 0-100 and is only
 * meaningful for candidates that already share a date of birth.
 */
public final class MatchScorer {

    public static final int DUPLICATE_THRESHOLD = 80;
    public static final int REVIEW_THRESHOLD = 50;

    private MatchScorer() {
    }

    public static int score(String firstName, String lastName, LocalDate dob, String phone, String postcode,
                            Patient existing) {
        if (!dob.equals(existing.getDateOfBirth())) {
            return 0;
        }
        int score = 0;
        int lastDistance = distance(norm(lastName), norm(existing.getLastName()));
        score += lastDistance == 0 ? 40 : lastDistance == 1 ? 25 : 0;
        String first = norm(firstName);
        String existingFirst = norm(existing.getFirstName());
        score += first.equals(existingFirst) ? 30 : first.charAt(0) == existingFirst.charAt(0) ? 10 : 0;
        if (phone != null && existing.getPhone() != null && phone.replaceAll("\\D", "").equals(existing.getPhone())) {
            score += 20;
        }
        if (postcode != null && existing.getPostcode() != null
                && postcode.replaceAll("\\s", "").equalsIgnoreCase(existing.getPostcode())) {
            score += 10;
        }
        return score;
    }

    private static String norm(String s) {
        return s.trim().toLowerCase(Locale.ROOT);
    }

    /** Levenshtein edit distance. */
    static int distance(String a, String b) {
        int[] prev = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            prev[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            int[] cur = new int[b.length() + 1];
            cur[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            prev = cur;
        }
        return prev[b.length()];
    }
}
