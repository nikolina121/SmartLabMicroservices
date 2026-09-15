package org.example.rentalreservationservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateMemberRequest {

    // NAPOMENA: externalUserId i memberType namerno NISU ovde. Oba se izvode iz
    // JWT tokena (X-User-Id / X-User-Roles header-a koje gateway ubacuje nakon
    // validacije potpisa) - inače bi klijent mogao da se ručno proglasi za ENGINEER-a.

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    private String memberCode;
}
