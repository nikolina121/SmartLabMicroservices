package org.example.rentalreservationservice.repository;

import org.example.rentalreservationservice.model.ReservationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ReservationRequestRepository extends JpaRepository<ReservationRequest, Long> {

    List<ReservationRequest> findByMemberId(Long memberId);

    @Query("select r.requestedComponentId as componentId, count(r) as requestCount from ReservationRequest r group by r.requestedComponentId order by count(r) desc")
    List<Object[]> mostRequestedComponents();
}
