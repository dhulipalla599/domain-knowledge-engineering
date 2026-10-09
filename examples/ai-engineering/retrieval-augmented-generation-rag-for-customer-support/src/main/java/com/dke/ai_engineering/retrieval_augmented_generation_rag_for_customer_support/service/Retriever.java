package com.dke.ai_engineering.retrieval_augmented_generation_rag_for_customer_support.service;

import com.dke.ai_engineering.retrieval_augmented_generation_rag_for_customer_support.domain.ArticleStatus;
import com.dke.ai_engineering.retrieval_augmented_generation_rag_for_customer_support.integration.EmbeddingClient;
import com.dke.ai_engineering.retrieval_augmented_generation_rag_for_customer_support.repository.KnowledgeArticleRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

/** Ranks PUBLISHED articles by cosine similarity to the question. Production: a pgvector nearest-neighbour query. */
@Service
public class Retriever {

    public record Passage(Long articleId, String title, String text, double score) {}

    private final KnowledgeArticleRepository articles;
    private final EmbeddingClient embeddings;

    public Retriever(KnowledgeArticleRepository articles, EmbeddingClient embeddings) {
        this.articles = articles;
        this.embeddings = embeddings;
    }

    public List<Passage> retrieve(String question, int topK) {
        double[] q = embeddings.embed(question);
        return articles.findByStatus(ArticleStatus.PUBLISHED).stream()
                .map(a -> new Passage(a.getId(), a.getTitle(), a.getBody(), cosine(q, embeddings.embed(a.getTitle() + " " + a.getBody()))))
                .sorted(Comparator.comparingDouble(Passage::score).reversed())
                .limit(topK)
                .toList();
    }

    static double cosine(double[] a, double[] b) {
        double dot = 0;
        for (int i = 0; i < a.length; i++) dot += a[i] * b[i];
        return dot;
    }
}
