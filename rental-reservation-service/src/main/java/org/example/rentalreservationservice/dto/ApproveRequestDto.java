package org.example.rentalreservationservice.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApproveRequestDto {

    private Long approvedByUserId;
    private Long inventoryItemId;
}
