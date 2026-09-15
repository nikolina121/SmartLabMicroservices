package org.example.rentalreservationservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateMemberRequest {
    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    private String memberCode;
}
