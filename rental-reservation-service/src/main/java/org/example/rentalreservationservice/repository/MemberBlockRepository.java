package org.example.rentalreservationservice.repository;

import org.example.rentalreservationservice.model.MemberBlock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberBlockRepository extends JpaRepository<MemberBlock, Long> {

    List<MemberBlock> findByMemberIdAndLiftedAtIsNull(Long memberId);
}
