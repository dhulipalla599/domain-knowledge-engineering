package com.dke.ai_engineering.retrieval_augmented_generation_rag_for_customer_support.repository;

import com.dke.ai_engineering.retrieval_augmented_generation_rag_for_customer_support.domain.ArticleStatus;
import com.dke.ai_engineering.retrieval_augmented_generation_rag_for_customer_support.domain.KnowledgeArticle;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KnowledgeArticleRepository extends JpaRepository<KnowledgeArticle, Long> {
    List<KnowledgeArticle> findByStatus(ArticleStatus status);
}
