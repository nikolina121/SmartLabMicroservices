package org.example.rentalreservationservice.feign;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StatusRequestDto {
    private String operationalStatus;
}
