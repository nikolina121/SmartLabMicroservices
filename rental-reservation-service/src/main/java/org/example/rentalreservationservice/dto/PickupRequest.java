package org.example.rentalreservationservice.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class PickupRequest {

    private LocalDateTime dueDate;
    private String conditionBefore;
    private Long issuedByUserId;
}
