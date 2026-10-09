package com.dke.ai_engineering.retrieval_augmented_generation_rag_for_customer_support.repository;

import com.dke.ai_engineering.retrieval_augmented_generation_rag_for_customer_support.domain.SupportQuery;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupportQueryRepository extends JpaRepository<SupportQuery, Long> {
    Optional<SupportQuery> findByIdempotencyKey(String idempotencyKey);
}
