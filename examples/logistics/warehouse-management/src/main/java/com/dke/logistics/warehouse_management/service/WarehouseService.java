package com.dke.logistics.warehouse_management.service;

import com.dke.logistics.warehouse_management.domain.Bin;
import com.dke.logistics.warehouse_management.domain.Receipt;
import com.dke.logistics.warehouse_management.domain.StockLevel;
import com.dke.logistics.warehouse_management.domain.WarehouseEvents.GoodsPutAway;
import com.dke.logistics.warehouse_management.domain.WarehouseEvents.GoodsReceived;
import com.dke.logistics.warehouse_management.domain.WarehouseException;
import com.dke.logistics.warehouse_management.domain.WarehouseException.Kind;
import com.dke.logistics.warehouse_management.repository.BinRepository;
import com.dke.logistics.warehouse_management.repository.ReceiptRepository;
import com.dke.logistics.warehouse_management.repository.StockLevelRepository;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WarehouseService {

    private final ReceiptRepository receipts;
    private final BinRepository bins;
    private final StockLevelRepository stock;
    private final ApplicationEventPublisher events;

    public WarehouseService(ReceiptRepository receipts, BinRepository bins, StockLevelRepository stock,
                            ApplicationEventPublisher events) {
        this.receipts = receipts;
        this.bins = bins;
        this.stock = stock;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public List<Receipt> listReceipts() {
        return receipts.findAll();
    }

    @Transactional(readOnly = true)
    public List<StockLevel> listStock() {
        return stock.findAll();
    }

    @Transactional
    public Receipt receive(Long receiptId, int countedQty) {
        Receipt receipt = findReceipt(receiptId);
        receipt.receive(countedQty);
        events.publishEvent(new GoodsReceived(receipt.getId(), receipt.getAsnNumber(), receipt.getSku(), countedQty));
        return receipt;
    }

    /** Key rule: goods may only be put away once counted, into a bin that is certified and has room. */
    @Transactional
    public Receipt putAway(Long receiptId, String binCode) {
        Receipt receipt = findReceipt(receiptId);
        Bin bin = bins.findById(binCode)
                .orElseThrow(() -> new WarehouseException(Kind.NOT_FOUND, "Bin %s not found".formatted(binCode)));
        receipt.putAway(binCode);
        bin.store(receipt.getReceivedQty(), receipt.isHazmat());
        stock.findBySkuAndBinCode(receipt.getSku(), binCode)
                .ifPresentOrElse(level -> level.add(receipt.getReceivedQty()),
                        () -> stock.save(new StockLevel(receipt.getSku(), binCode, receipt.getReceivedQty())));
        events.publishEvent(new GoodsPutAway(receipt.getId(), receipt.getSku(), binCode, receipt.getReceivedQty()));
        return receipt;
    }

    private Receipt findReceipt(Long id) {
        return receipts.findById(id)
                .orElseThrow(() -> new WarehouseException(Kind.NOT_FOUND, "Receipt %d not found".formatted(id)));
    }
}
