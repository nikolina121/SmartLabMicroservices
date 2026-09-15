package org.example.rentalreservationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ComponentDemandDto {
    private Long componentId;
    private Long requestCount;
}
