package org.example.rentalreservationservice.feign;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InventoryItemDto {
    private Long id;
    private String itemTag;
    private Long componentId;
    private String operationalStatus;
    private Integer conditionRating;
}
