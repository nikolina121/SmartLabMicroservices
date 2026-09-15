package org.example.rentalreservationservice.repository;

import jakarta.persistence.LockModeType;
import org.example.rentalreservationservice.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reservation r where r.inventoryItemId = ?1 and r.status in ('APPROVED', 'ACTIVE') and r.reservationStart < ?3 and r.reservationEnd > ?2")
    List<Reservation> findOverlaps(Long inventoryItemId, LocalDateTime start, LocalDateTime end);

    List<Reservation> findByStatusAndPickupDeadlineBefore(String status, LocalDateTime now);

    List<Reservation> findByRequestId(Long requestId);
}
