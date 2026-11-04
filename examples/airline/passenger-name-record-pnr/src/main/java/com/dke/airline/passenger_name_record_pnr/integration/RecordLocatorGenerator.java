package com.dke.airline.passenger_name_record_pnr.integration;

/** Produces the 6-character booking reference. Production: a collision-checked random generator in the PNR service. */
public interface RecordLocatorGenerator {
    String next();
}
