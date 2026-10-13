package com.dke.insurance.quote_and_rating.api;

import com.dke.insurance.quote_and_rating.service.QuoteService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/quotes")
public class QuoteController {

    private final QuoteService service;

    public QuoteController(QuoteService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<QuoteResponse> create(@Valid @RequestBody CreateQuoteRequest request) {
        QuoteResponse body = QuoteResponse.from(service.createQuote(request));
        return ResponseEntity.created(URI.create("/api/quotes/" + body.id())).body(body);
    }

    @GetMapping("/{id}")
    public QuoteResponse get(@PathVariable UUID id) {
        return QuoteResponse.from(service.find(id));
    }

    @PostMapping("/{id}/bind")
    public QuoteResponse bind(@PathVariable UUID id) {
        return QuoteResponse.from(service.bind(id));
    }
}
