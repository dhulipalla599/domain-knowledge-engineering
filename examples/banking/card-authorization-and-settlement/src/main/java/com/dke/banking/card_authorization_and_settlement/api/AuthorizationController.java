package com.dke.banking.card_authorization_and_settlement.api;

import com.dke.banking.card_authorization_and_settlement.service.AuthorizationService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthorizationController {

    private final AuthorizationService service;

    public AuthorizationController(AuthorizationService service) {
        this.service = service;
    }

    /** Approve or decline. A decline is a valid business answer, so both return 200 with a response code. */
    @PostMapping("/authorizations")
    public AuthorizationResponse authorize(@Valid @RequestBody AuthorizationRequest req) {
        return AuthorizationResponse.from(service.authorize(req.networkReference(), req.cardToken(),
                req.merchantName(), req.mcc(), req.amount(), req.currency()));
    }

    @PostMapping("/authorizations/{id}/reversal")
    public AuthorizationResponse reverse(@PathVariable UUID id) {
        return AuthorizationResponse.from(service.reverse(id));
    }

    @PostMapping("/authorizations/{id}/clearing")
    public AuthorizationResponse clear(@PathVariable UUID id, @Valid @RequestBody ClearingRequest req) {
        return AuthorizationResponse.from(service.clear(id, req.amount()));
    }

    @GetMapping("/authorizations/{id}")
    public AuthorizationResponse get(@PathVariable UUID id) {
        return AuthorizationResponse.from(service.findAuthorization(id));
    }

    @GetMapping("/cards/{cardToken}")
    public CardResponse card(@PathVariable String cardToken) {
        return CardResponse.from(service.findCard(cardToken));
    }

    @GetMapping("/cards/{cardToken}/authorizations")
    public List<AuthorizationResponse> history(@PathVariable String cardToken) {
        return service.historyFor(cardToken).stream().map(AuthorizationResponse::from).toList();
    }
}
