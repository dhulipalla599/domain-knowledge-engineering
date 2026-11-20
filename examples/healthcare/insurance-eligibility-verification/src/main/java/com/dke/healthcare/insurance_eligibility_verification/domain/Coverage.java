package com.dke.healthcare.insurance_eligibility_verification.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.LocalDate;

/** The insurance a patient presented at registration: payer, member ID and subscriber demographics. */
@Entity
public class Coverage {

    @Id
    private String id;
    private String patientId;
    private String payerId;
    private String memberId;
    private String subscriberFirstName;
    private String subscriberLastName;
    private LocalDate subscriberDateOfBirth;

    protected Coverage() {
    }

    public Coverage(String id, String patientId, String payerId, String memberId,
                    String subscriberFirstName, String subscriberLastName, LocalDate subscriberDateOfBirth) {
        this.id = id;
        this.patientId = patientId;
        this.payerId = payerId;
        this.memberId = memberId;
        this.subscriberFirstName = subscriberFirstName;
        this.subscriberLastName = subscriberLastName;
        this.subscriberDateOfBirth = subscriberDateOfBirth;
    }

    public String getId() { return id; }
    public String getPatientId() { return patientId; }
    public String getPayerId() { return payerId; }
    public String getMemberId() { return memberId; }
    public String getSubscriberFirstName() { return subscriberFirstName; }
    public String getSubscriberLastName() { return subscriberLastName; }
    public LocalDate getSubscriberDateOfBirth() { return subscriberDateOfBirth; }
}
