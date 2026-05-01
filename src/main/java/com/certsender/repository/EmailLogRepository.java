package com.certsender.repository;

import com.certsender.entity.EmailLog;
import com.certsender.entity.EmailLog.EmailStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {
    List<EmailLog> findAllByOrderBySentAtDesc();
    List<EmailLog> findByStatus(EmailStatus status);
    List<EmailLog> findBySessionIdOrderBySentAtDesc(String sessionId);
    long countByStatus(EmailStatus status);
}
