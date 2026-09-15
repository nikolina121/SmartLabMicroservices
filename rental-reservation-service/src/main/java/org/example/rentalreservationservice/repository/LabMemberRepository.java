package org.example.rentalreservationservice.repository;

import org.example.rentalreservationservice.model.LabMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LabMemberRepository extends JpaRepository<LabMember, Long> {

    Optional<LabMember> findByExternalUserId(Long externalUserId);
}
