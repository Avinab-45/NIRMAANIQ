package com.construction.dto.ml;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * Individual object detection prediction returned by YOLO11 FastAPI model.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class MlDetection {

    @JsonAlias({"class", "class_name", "label", "category", "name"})
    private String className;

    @JsonAlias({"confidence", "score", "conf"})
    private Double confidence;

    @JsonAlias({"box", "bbox", "bounding_box", "coordinates"})
    private Object box;

    private Map<String, Object> attributes;
}
