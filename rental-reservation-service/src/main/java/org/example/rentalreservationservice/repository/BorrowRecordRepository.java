package org.example.rentalreservationservice.repository;

import org.example.rentalreservationservice.model.BorrowRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BorrowRecordRepository extends JpaRepository<BorrowRecord, Long> {

    Optional<BorrowRecord> findByReservationId(Long reservationId);

    List<BorrowRecord> findByStatus(String status);

    List<BorrowRecord> findByStatusAndDueDateBefore(String status, LocalDateTime now);

    List<BorrowRecord> findByStatusAndDueDateBetween(String status, LocalDateTime from, LocalDateTime to);

    @Query("select count(b) from BorrowRecord b, Reservation r where b.reservationId = r.id and r.memberId = ?1 and b.status in ('ACTIVE', 'OVERDUE')")
    long countActiveOrOverdueForMember(Long memberId);

    @Query("select count(b) from BorrowRecord b, Reservation r where b.reservationId = r.id and r.memberId = ?1 and b.status = 'OVERDUE'")
    long countOverdueForMember(Long memberId);

    @Query("select b from BorrowRecord b, Reservation r where b.reservationId = r.id and r.memberId = ?1 and b.status = 'OVERDUE'")
    List<BorrowRecord> findOverdueForMember(Long memberId);

    @Query("select distinct r.memberId from BorrowRecord b, Reservation r where b.reservationId = r.id and b.status in ('ACTIVE', 'OVERDUE')")
    List<Long> findMemberIdsWithActiveBorrows();
}
