package com.dke.ai_engineering.retrieval_augmented_generation_rag_for_customer_support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dke.ai_engineering.retrieval_augmented_generation_rag_for_customer_support.domain.QueryStatus;
import com.dke.ai_engineering.retrieval_augmented_generation_rag_for_customer_support.domain.SupportQuery;
import com.dke.ai_engineering.retrieval_augmented_generation_rag_for_customer_support.integration.ExtractiveLlmClient;
import com.dke.ai_engineering.retrieval_augmented_generation_rag_for_customer_support.integration.HashedEmbeddingClient;
import com.dke.ai_engineering.retrieval_augmented_generation_rag_for_customer_support.repository.KnowledgeArticleRepository;
import com.dke.ai_engineering.retrieval_augmented_generation_rag_for_customer_support.repository.SupportQueryRepository;
import com.dke.ai_engineering.retrieval_augmented_generation_rag_for_customer_support.service.PiiRedactor;
import com.dke.ai_engineering.retrieval_augmented_generation_rag_for_customer_support.service.RagService;
import com.dke.ai_engineering.retrieval_augmented_generation_rag_for_customer_support.service.Retriever;
import com.dke.ai_engineering.retrieval_augmented_generation_rag_for_customer_support.service.Retriever.Passage;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;

/** Unit tests of the core rules: grounding/escalation, citation validation, redaction and the state machine. */
@SpringBootTest
class RagServiceTest {

    @Autowired RagService rag;

    @Test
    void relevantQuestionIsAnsweredWithCitation() {
        SupportQuery q = rag.ask("c-1", "Can I get a refund for an unused data add-on?", null);
        assertThat(q.getStatus()).isEqualTo(QueryStatus.ANSWERED);
        assertThat(q.getAnswer()).contains("14 days").contains("[KB-1]");
        assertThat(q.getCitedArticleIds()).containsExactly(1L);
    }

    @Test
    void unrelatedQuestionIsEscalatedWithoutCallingTheModel() {
        SupportQuery q = rag.ask("c-1", "What is the meaning of life?", null);
        assertThat(q.getStatus()).isEqualTo(QueryStatus.ESCALATED);
        assertThat(q.getAnswer()).isNull();
    }

    @Test
    void draftArticlesAreNeverRetrieved() {
        SupportQuery q = rag.ask("c-1", "How much does 5G Home Internet cost per month?", null);
        assertThat(q.getCitedArticleIds()).doesNotContain(6L);
        assertThat(q.getAnswer() == null || !q.getAnswer().contains("40")).isTrue();
    }

    @Test
    void sameIdempotencyKeyReturnsTheSameQuery() {
        SupportQuery a = rag.ask("c-1", "How do I change my payment method?", "key-1");
        SupportQuery b = rag.ask("c-1", "How do I change my payment method?", "key-1");
        assertThat(b.getId()).isEqualTo(a.getId());
        assertThatThrownBy(() -> rag.ask("c-1", "A different question about roaming", "key-1"))
                .isInstanceOf(RagService.IdempotencyConflictException.class);
    }

    @Test
    void inventedCitationsAreRejected() {
        List<Passage> passages = List.of(new Passage(1L, "t", "x", 0.9));
        assertThat(RagService.validCitations("Yes [KB-1]", passages)).containsExactly(1L);
        assertThat(RagService.validCitations("Yes [KB-99]", passages)).isEmpty();
        assertThat(RagService.validCitations("Yes, trust me.", passages)).isEmpty();
    }

    @Test
    void piiIsRedactedBeforeStorage() {
        String out = new PiiRedactor().redact("Mail maya@example.com, card 4111 1111 1111 1111");
        assertThat(out).isEqualTo("Mail [EMAIL], card [CARD]");
    }

    @Test
    void terminalStatesCannotMove() {
        SupportQuery q = new SupportQuery("c-1", "q", null);
        q.escalate("test");
        assertThatThrownBy(q::resolve).isInstanceOf(IllegalStateException.class);
    }
}
