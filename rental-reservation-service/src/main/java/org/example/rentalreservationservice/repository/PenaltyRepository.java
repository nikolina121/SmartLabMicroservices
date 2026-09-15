package org.example.rentalreservationservice.repository;

import org.example.rentalreservationservice.model.Penalty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PenaltyRepository extends JpaRepository<Penalty, Long> {

    boolean existsByMemberIdAndStatus(Long memberId, String status);

    boolean existsByBorrowRecordIdAndStatus(Long borrowRecordId, String status);

    Optional<Penalty> findFirstByBorrowRecordIdAndStatus(Long borrowRecordId, String status);

    long countByMemberIdAndStatus(Long memberId, String status);

    List<Penalty> findByStatus(String status);

    List<Penalty> findByMemberId(Long memberId);
}
