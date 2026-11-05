package com.dke.healthcare.electronic_health_records_ehr.integration;

import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class StubCareTeamDirectory implements CareTeamDirectory {

    private final Map<String, Set<String>> careTeams = Map.of(
            "dr-lee", Set.of("pat-amara", "pat-jonas"),
            "dr-patel", Set.of("pat-priya"));

    @Override
    public boolean hasTreatmentRelationship(String clinicianId, String patientId) {
        return careTeams.getOrDefault(clinicianId, Set.of()).contains(patientId);
    }
}
