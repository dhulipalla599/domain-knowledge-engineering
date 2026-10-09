package com.dke.ai_engineering.retrieval_augmented_generation_rag_for_customer_support.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "knowledge_article")
public class KnowledgeArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 2000)
    private String body;

    @Column(nullable = false)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ArticleStatus status = ArticleStatus.PUBLISHED;

    protected KnowledgeArticle() {}

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public String getCategory() { return category; }
    public ArticleStatus getStatus() { return status; }
}
