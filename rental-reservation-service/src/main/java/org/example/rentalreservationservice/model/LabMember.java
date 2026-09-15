package org.example.rentalreservationservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "lab_members")
public class LabMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_user_id", nullable = false, unique = true)
    private Long externalUserId;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(name = "member_code", nullable = false, unique = true, length = 30)
    private String memberCode;

    @Column(name = "member_type", nullable = false, length = 20)
    private String memberType = "STUDENT";

    @Column(name = "membership_status", nullable = false, length = 20)
    private String membershipStatus = "ACTIVE";

    @Column(name = "blocked_until")
    private LocalDateTime blockedUntil;

    @Column(name = "block_reason")
    private String blockReason;

    @Column(name = "penalty_points", nullable = false)
    private Integer penaltyPoints = 0;

    @Column(name = "joined_at", insertable = false, updatable = false)
    private LocalDateTime joinedAt;
}
