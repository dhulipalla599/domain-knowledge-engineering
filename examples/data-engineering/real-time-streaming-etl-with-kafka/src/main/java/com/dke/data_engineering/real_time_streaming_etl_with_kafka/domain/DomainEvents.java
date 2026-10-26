package com.dke.data_engineering.real_time_streaming_etl_with_kafka.domain;

/** In-process stand-ins for Kafka topics; the record name matches the topic it replaces. */
public final class DomainEvents {

    private DomainEvents() {
    }

    /** Topic orders.raw - a batch of raw lines was extracted. */
    public record OrdersRaw(Long runId, String source, int lineCount) {
    }

    /** Topic orders.curated - one validated order was loaded. Keyed by orderId in Kafka. */
    public record OrdersCurated(Long runId, String eventId, String orderId, String country,
                                String currency, long amountCents) {
    }

    /** Topic orders.dlq - one raw line was rejected. */
    public record OrdersDlq(Long runId, String rawLine, String reason) {
    }

    /** Topic pipeline.run-completed - run finished with its final metrics. */
    public record PipelineRunCompleted(Long runId, RunStatus status, int loaded, int duplicates, int rejected) {
    }
}
