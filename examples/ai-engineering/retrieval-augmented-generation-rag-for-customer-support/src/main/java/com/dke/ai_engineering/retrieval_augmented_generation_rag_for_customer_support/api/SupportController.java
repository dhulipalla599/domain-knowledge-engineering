package com.dke.ai_engineering.retrieval_augmented_generation_rag_for_customer_support.api;

import com.dke.ai_engineering.retrieval_augmented_generation_rag_for_customer_support.api.SupportDtos.*;
import com.dke.ai_engineering.retrieval_augmented_generation_rag_for_customer_support.repository.KnowledgeArticleRepository;
import com.dke.ai_engineering.retrieval_augmented_generation_rag_for_customer_support.service.RagService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/support")
public class SupportController {

    private final RagService rag;
    private final KnowledgeArticleRepository articles;

    public SupportController(RagService rag, KnowledgeArticleRepository articles) {
        this.rag = rag;
        this.articles = articles;
    }

    @PostMapping("/queries")
    public ResponseEntity<QueryResponse> ask(@Valid @RequestBody AskRequest request,
                                             @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        QueryResponse body = QueryResponse.from(rag.ask(request.customerId(), request.question(), idempotencyKey));
        return ResponseEntity.created(URI.create("/api/support/queries/" + body.id())).body(body);
    }

    @GetMapping("/queries/{id}")
    public QueryResponse get(@PathVariable Long id) {
        return QueryResponse.from(rag.get(id));
    }

    @PostMapping("/queries/{id}/feedback")
    public QueryResponse feedback(@PathVariable Long id, @Valid @RequestBody FeedbackRequest request) {
        return QueryResponse.from(rag.feedback(id, request.helpful()));
    }

    @GetMapping("/articles")
    public List<ArticleResponse> articles() {
        return articles.findAll().stream().map(ArticleResponse::from).toList();
    }
}
