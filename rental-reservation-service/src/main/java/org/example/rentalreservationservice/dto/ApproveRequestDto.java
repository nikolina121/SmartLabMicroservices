package org.example.rentalreservationservice.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApproveRequestDto {

    private Long approvedByUserId;

    /** Opciono: inženjer ručno dodeljuje konkretan (alternativni) uređaj umesto automatskog izbora. */
    private Long inventoryItemId;
}
