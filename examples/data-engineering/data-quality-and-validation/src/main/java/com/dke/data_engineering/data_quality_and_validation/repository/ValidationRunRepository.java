package com.dke.data_engineering.data_quality_and_validation.repository;

import com.dke.data_engineering.data_quality_and_validation.domain.RunStatus;
import com.dke.data_engineering.data_quality_and_validation.domain.ValidationRun;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ValidationRunRepository extends JpaRepository<ValidationRun, Long> {

    Optional<ValidationRun> findFirstByChecksumAndStatusNot(String checksum, RunStatus status);

    List<ValidationRun> findAllByOrderByIdDesc();
}
