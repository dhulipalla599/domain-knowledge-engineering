package com.dke.healthcare.appointment_scheduling.repository;

import com.dke.healthcare.appointment_scheduling.domain.Provider;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProviderRepository extends JpaRepository<Provider, String> {
}
