package com.dke.airline.passenger_name_record_pnr.repository;

import com.dke.airline.passenger_name_record_pnr.domain.Pnr;
import com.dke.airline.passenger_name_record_pnr.domain.PnrStatus;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PnrRepository extends JpaRepository<Pnr, Long> {

    Optional<Pnr> findByRecordLocator(String recordLocator);

    /** True when the same passenger already holds a non-cancelled PNR on the same flight and date. */
    @Query("""
            select count(p) > 0 from Pnr p join p.passengers pax join p.segments s
            where p.status <> :excluded and pax.firstName = :firstName and pax.lastName = :lastName
              and s.flightNumber = :flightNumber and s.departureDate = :date
            """)
    boolean existsActiveBooking(@Param("firstName") String firstName, @Param("lastName") String lastName,
                                @Param("flightNumber") String flightNumber, @Param("date") LocalDate date,
                                @Param("excluded") PnrStatus excluded);
}
