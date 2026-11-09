package com.dke.data_engineering.batch_etl_and_workflow_orchestration.domain;

/** One untyped source line, exactly as read from the file. Validation happens in the transform step. */
public record RawSale(String orderId, String customerId, String sku,
                      String quantity, String unitPrice, String currency) { }
