package com.dke.retail.product_catalog_and_pricing.service;

import com.dke.retail.product_catalog_and_pricing.domain.PriceChange;
import com.dke.retail.product_catalog_and_pricing.domain.PriceChangeStatus;
import com.dke.retail.product_catalog_and_pricing.domain.PricingEvents.PriceChangeRejected;
import com.dke.retail.product_catalog_and_pricing.domain.PricingEvents.PriceChangeRequested;
import com.dke.retail.product_catalog_and_pricing.domain.PricingEvents.ProductPriceUpdated;
import com.dke.retail.product_catalog_and_pricing.domain.Product;
import com.dke.retail.product_catalog_and_pricing.repository.PriceChangeRepository;
import com.dke.retail.product_catalog_and_pricing.repository.ProductRepository;
import com.dke.retail.product_catalog_and_pricing.service.PricingPolicy.Decision;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PricingService {

    private final ProductRepository products;
    private final PriceChangeRepository changes;
    private final PricingPolicy policy;
    private final ApplicationEventPublisher events;

    public PricingService(ProductRepository products, PriceChangeRepository changes,
                          PricingPolicy policy, ApplicationEventPublisher events) {
        this.products = products;
        this.changes = changes;
        this.policy = policy;
        this.events = events;
    }

    @Transactional
    public PriceChange requestChange(String sku, BigDecimal proposed, String reason) {
        Product product = products.findById(sku)
                .orElseThrow(() -> new PricingException.NotFound("Unknown SKU " + sku));
        if (changes.existsBySkuAndStatus(sku, PriceChangeStatus.PENDING_APPROVAL)) {
            throw new PricingException.Conflict("SKU " + sku + " already has a price change awaiting approval");
        }
        Decision decision = policy.evaluate(product, proposed);
        PriceChange change = changes.save(new PriceChange(
                sku, product.getPrice(), proposed.setScale(2, RoundingMode.HALF_UP), reason));
        if (decision == Decision.AUTO_APPLY) {
            apply(change, product);
        } else {
            events.publishEvent(new PriceChangeRequested(
                    change.getId(), sku, change.getOldPrice(), change.getNewPrice()));
        }
        return change;
    }

    @Transactional
    public PriceChange approve(Long changeId) {
        PriceChange change = find(changeId);
        Product product = products.findById(change.getSku()).orElseThrow();
        // Re-check the rules: cost may have changed while the request waited.
        policy.evaluate(product, change.getNewPrice());
        apply(change, product);
        return change;
    }

    @Transactional
    public PriceChange reject(Long changeId) {
        PriceChange change = find(changeId);
        change.moveTo(PriceChangeStatus.REJECTED);
        events.publishEvent(new PriceChangeRejected(change.getId(), change.getSku()));
        return change;
    }

    @Transactional(readOnly = true)
    public List<PriceChange> list() {
        return changes.findAll();
    }

    @Transactional(readOnly = true)
    public Product getProduct(String sku) {
        return products.findById(sku).orElseThrow(() -> new PricingException.NotFound("Unknown SKU " + sku));
    }

    @Transactional(readOnly = true)
    public List<Product> listProducts() {
        return products.findAll();
    }

    private void apply(PriceChange change, Product product) {
        change.moveTo(PriceChangeStatus.APPLIED);
        BigDecimal old = product.getPrice();
        product.changePrice(change.getNewPrice());
        events.publishEvent(new ProductPriceUpdated(product.getSku(), old, change.getNewPrice(), product.getCurrency()));
    }

    private PriceChange find(Long id) {
        return changes.findById(id).orElseThrow(() -> new PricingException.NotFound("Unknown price change " + id));
    }
}
