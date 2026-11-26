package com.construction.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuilderActivationRequest {

    @NotBlank(message = "Activation/Invitation code is required")
    private String activationCode;

    private String companyName;
    private String licenseNumber;
}
