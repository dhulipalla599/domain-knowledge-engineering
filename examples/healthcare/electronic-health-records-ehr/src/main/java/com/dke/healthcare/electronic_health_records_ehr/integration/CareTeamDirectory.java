package com.dke.healthcare.electronic_health_records_ehr.integration;

/** Port to the hospital's care-team / scheduling system. Production: an adapter over the MPI and ADT feed. */
public interface CareTeamDirectory {

    boolean hasTreatmentRelationship(String clinicianId, String patientId);
}
