package org.example.rentalreservationservice.repository;

import org.example.rentalreservationservice.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByMemberId(Long memberId);

    boolean existsByMemberIdAndTypeAndCreatedAtAfter(Long memberId, String type, java.time.LocalDateTime after);
}
