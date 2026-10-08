package com.dke.healthcare.patient_registration_and_master_patient_index.integration;

import com.dke.healthcare.patient_registration_and_master_patient_index.domain.MatchReviewRequested;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Stand-in for the data-steward work-queue consumer of patient.match-review-requested. */
@Component
public class ReviewTaskListener {

    private static final Logger log = LoggerFactory.getLogger(ReviewTaskListener.class);

    @EventListener
    public void on(MatchReviewRequested event) {
        log.info("steward task: compare {} with {} (score {})", event.mrn(), event.candidateMrn(), event.score());
    }
}
