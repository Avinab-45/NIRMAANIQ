package com.construction.dto.member;

import com.construction.entity.enums.ProjectMemberRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddMemberRequest {

    @NotNull(message = "Member role is required (CONTRACTOR or CLIENT)")
    private ProjectMemberRole role;

    @NotBlank(message = "Registered email is required for member assignment")
    @Email(message = "Invalid email format")
    private String email;

    // Optional contact phone number
    private String phoneNumber;
}
