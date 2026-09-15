package org.example.rentalreservationservice.repository;

import org.example.rentalreservationservice.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @Query(value = "SELECT * FROM reservations " +
            "WHERE inventory_item_id = :itemId " +
            "AND status IN ('APPROVED','ACTIVE') " +
            "AND reservation_start < :end AND reservation_end > :start " +
            "FOR UPDATE",
            nativeQuery = true)
    List<Reservation> findOverlaps(@Param("itemId") Long inventoryItemId,
                                   @Param("start") LocalDateTime start,
                                   @Param("end") LocalDateTime end);

    List<Reservation> findByStatusAndPickupDeadlineBefore(String status, LocalDateTime now);

    List<Reservation> findByRequestId(Long requestId);
}