package com.construction.dto.project;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateProjectRequest {

    @NotBlank(message = "Project name is required")
    private String name;

    private String description;
    private String location;
    private LocalDate plannedStartDate;
    private LocalDate plannedEndDate;
}
