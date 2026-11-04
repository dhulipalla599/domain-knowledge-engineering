package com.dke.retail.product_catalog_and_pricing.api;

import com.dke.retail.product_catalog_and_pricing.domain.PriceChange;
import com.dke.retail.product_catalog_and_pricing.domain.Product;
import com.dke.retail.product_catalog_and_pricing.service.PricingService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class PricingController {

    private final PricingService service;

    public PricingController(PricingService service) {
        this.service = service;
    }

    @GetMapping("/products")
    public List<ProductResponse> products() {
        return service.listProducts().stream().map(ProductResponse::from).toList();
    }

    @GetMapping("/products/{sku}")
    public ProductResponse product(@PathVariable String sku) {
        return ProductResponse.from(service.getProduct(sku));
    }

    @PostMapping("/price-changes")
    @ResponseStatus(HttpStatus.CREATED)
    public PriceChangeResponse request(@Valid @RequestBody PriceChangeRequest request) {
        return PriceChangeResponse.from(service.requestChange(request.sku(), request.newPrice(), request.reason()));
    }

    @PostMapping("/price-changes/{id}/approve")
    public PriceChangeResponse approve(@PathVariable Long id) {
        return PriceChangeResponse.from(service.approve(id));
    }

    @PostMapping("/price-changes/{id}/reject")
    public PriceChangeResponse reject(@PathVariable Long id) {
        return PriceChangeResponse.from(service.reject(id));
    }

    @GetMapping("/price-changes")
    public List<PriceChangeResponse> list() {
        return service.list().stream().map(PriceChangeResponse::from).toList();
    }
}
