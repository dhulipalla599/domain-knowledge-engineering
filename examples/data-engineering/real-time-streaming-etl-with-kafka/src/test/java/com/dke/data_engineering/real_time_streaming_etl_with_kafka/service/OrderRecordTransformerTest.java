package com.dke.data_engineering.real_time_streaming_etl_with_kafka.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dke.data_engineering.real_time_streaming_etl_with_kafka.domain.OrderFact;
import org.junit.jupiter.api.Test;

class OrderRecordTransformerTest {

    private final OrderRecordTransformer transformer = new OrderRecordTransformer();

    @Test
    void normalisesCodesAndConvertsAmountToMinorUnits() {
        OrderFact fact = transformer.transform("E-1,O-1,C-1,gb,120.05,gbp,2026-10-20T09:00:00Z");
        assertThat(fact.getCountry()).isEqualTo("GB");
        assertThat(fact.getCurrency()).isEqualTo("GBP");
        assertThat(fact.getAmountCents()).isEqualTo(12005);
    }

    @Test
    void rejectsNonPositiveAmount() {
        assertThatThrownBy(() -> transformer.transform("E-1,O-1,C-1,US,0,USD,2026-10-20T09:00:00Z"))
                .isInstanceOf(InvalidRecordException.class).hasMessageContaining("positive");
    }

    @Test
    void rejectsSubCentPrecisionAndUnknownCurrency() {
        assertThatThrownBy(() -> transformer.transform("E-1,O-1,C-1,US,1.234,USD,2026-10-20T09:00:00Z"))
                .isInstanceOf(InvalidRecordException.class);
        assertThatThrownBy(() -> transformer.transform("E-1,O-1,C-1,US,1.00,JPY,2026-10-20T09:00:00Z"))
                .hasMessageContaining("currency");
    }

    @Test
    void rejectsWrongFieldCount() {
        assertThatThrownBy(() -> transformer.transform("E-1,O-1"))
                .isInstanceOf(InvalidRecordException.class).hasMessageContaining("7 fields");
    }
}
