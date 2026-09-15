package org.example.rentalreservationservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RejectRequestDto {

    @NotBlank
    private String reason;
}
