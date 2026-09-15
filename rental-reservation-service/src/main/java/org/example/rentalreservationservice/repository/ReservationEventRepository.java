package org.example.rentalreservationservice.repository;

import org.example.rentalreservationservice.model.ReservationEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservationEventRepository extends JpaRepository<ReservationEvent, Long> {

    List<ReservationEvent> findByReservationId(Long reservationId);
}
