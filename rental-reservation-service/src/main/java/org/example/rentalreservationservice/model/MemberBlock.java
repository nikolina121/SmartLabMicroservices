package org.example.rentalreservationservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "member_blocks")
public class MemberBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private String reason;

    @Column(name = "blocked_from", nullable = false)
    private LocalDateTime blockedFrom = LocalDateTime.now();

    @Column(name = "blocked_until")
    private LocalDateTime blockedUntil;

    @Column(name = "lifted_at")
    private LocalDateTime liftedAt;

    @Column(name = "created_by_user_id")
    private Long createdByUserId;
}
