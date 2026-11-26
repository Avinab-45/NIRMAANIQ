package com.construction.dto.project;

import com.construction.entity.enums.ProjectStatus;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProjectRequest {
    private String name;
    private String description;
    private String location;
    private ProjectStatus status;
    private LocalDate plannedStartDate;
    private LocalDate plannedEndDate;
    private LocalDate actualStartDate;
    private LocalDate actualEndDate;
}
