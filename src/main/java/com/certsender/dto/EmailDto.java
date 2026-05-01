package com.certsender.dto;

import com.certsender.entity.EmailLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

public class EmailDto {

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SendSummary {
        private int totalFiles;
        private int successCount;
        private int failureCount;
        private List<SendResult> results;
        private String sessionId;
        private LocalDateTime startedAt;
        private LocalDateTime finishedAt;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SendResult {
        private String email;
        private String fileName;
        private boolean success;
        private String message;
        private LocalDateTime sentAt;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DashboardStats {
        private long totalSent;
        private long successCount;
        private long failureCount;
        private long pendingCount;
        private double successRate;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class LogResponse {
        private Long id;
        private String studentEmail;
        private String fileName;
        private LocalDateTime sentAt;
        private String status;
        private String errorMessage;
        private String sessionId;

        public static LogResponse fromEntity(EmailLog log) {
            return LogResponse.builder()
                .id(log.getId())
                .studentEmail(log.getStudentEmail())
                .fileName(log.getFileName())
                .sentAt(log.getSentAt())
                .status(log.getStatus().name())
                .errorMessage(log.getErrorMessage())
                .sessionId(log.getSessionId())
                .build();
        }
    }
}
