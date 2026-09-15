package org.example.rentalreservationservice.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReturnRequest {

    private String conditionAfter;
    private String returnNotes;
    private boolean damaged;
    private String issueCategory;
    private Long receivedByUserId;
}
