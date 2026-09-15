package org.example.rentalreservationservice.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CreateRequestDto {

    @NotNull
    private Long componentId;

    @NotNull
    private LocalDateTime start;

    @NotNull
    private LocalDateTime end;

    @Min(1)
    private int quantity = 1;

    private String purpose;
}
