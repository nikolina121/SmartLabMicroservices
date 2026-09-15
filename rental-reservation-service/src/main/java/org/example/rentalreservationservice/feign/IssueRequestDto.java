package org.example.rentalreservationservice.feign;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IssueRequestDto {
    private Long inventoryItemId;
    private Long reportedByUserId;
    private String issueCategory;
    private String description;
}
