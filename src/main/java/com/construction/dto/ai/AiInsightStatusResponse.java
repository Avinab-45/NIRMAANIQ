package com.construction.dto.ai;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiInsightStatusResponse<T> {
    private String status; // "AVAILABLE", "NOT_YET_GENERATED", "UNAVAILABLE_NO_PROVIDER_CONFIGURED"
    private String message;
    private T insights;
    private LocalDateTime timestamp;

    public static <T> AiInsightStatusResponse<T> available(T data, String message) {
        return AiInsightStatusResponse.<T>builder()
                .status("AVAILABLE")
                .message(message)
                .insights(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> AiInsightStatusResponse<T> notYetGenerated(String message) {
        return AiInsightStatusResponse.<T>builder()
                .status("NOT_YET_GENERATED")
                .message(message)
                .insights(null)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> AiInsightStatusResponse<T> unavailable(String message) {
        return AiInsightStatusResponse.<T>builder()
                .status("UNAVAILABLE_NO_PROVIDER_CONFIGURED")
                .message(message)
                .insights(null)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
