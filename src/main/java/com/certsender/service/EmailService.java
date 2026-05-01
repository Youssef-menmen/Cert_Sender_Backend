package com.certsender.service;

import com.certsender.dto.EmailDto;
import com.certsender.entity.EmailLog;
import com.certsender.entity.EmailLog.EmailStatus;
import com.certsender.repository.EmailLogRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final EmailLogRepository emailLogRepository;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.mail.from-name:Administration}")
    private String fromName;

    @Value("${app.mail.delay-between-emails:500}")
    private long delayBetweenEmails;

    // ---- Méthode principale : traite tous les PDF ----
    public EmailDto.SendSummary sendCertificates(
            MultipartFile[] files,
            String emailSubject,
            String emailBody) {

        String sessionId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("=== DÉBUT SESSION [{}] - {} fichiers ===", sessionId, files.length);

        LocalDateTime startedAt = LocalDateTime.now();
        List<EmailDto.SendResult> results = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;

        for (MultipartFile file : files) {
            EmailDto.SendResult result = processSinglePdf(file, emailSubject, emailBody, sessionId);
            results.add(result);
            if (result.isSuccess()) successCount++;
            else failureCount++;

            // Pause entre chaque email pour éviter le blocage Gmail
            if (delayBetweenEmails > 0) {
                try {
                    Thread.sleep(delayBetweenEmails);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        log.info("=== FIN SESSION [{}] : {} succès, {} échecs ===", sessionId, successCount, failureCount);

        return EmailDto.SendSummary.builder()
                .totalFiles(files.length)
                .successCount(successCount)
                .failureCount(failureCount)
                .results(results)
                .sessionId(sessionId)
                .startedAt(startedAt)
                .finishedAt(LocalDateTime.now())
                .build();
    }

    // ---- Traite un seul fichier PDF ----
    private EmailDto.SendResult processSinglePdf(
            MultipartFile file,
            String emailSubject,
            String emailBody,
            String sessionId) {

        String fileName = file.getOriginalFilename();
        log.info("Traitement : {}", fileName);

        String studentEmail = extractEmailFromFileName(fileName);

        if (studentEmail == null) {
            String errorMsg = "Nom de fichier invalide (doit être email@domaine.ext.pdf) : " + fileName;
            log.warn(errorMsg);
            saveLog(null, fileName, EmailStatus.FAILED, errorMsg, sessionId);
            return EmailDto.SendResult.builder()
                    .email("inconnu").fileName(fileName)
                    .success(false).message(errorMsg)
                    .sentAt(LocalDateTime.now()).build();
        }

        try {
            String personalizedBody = personalizeEmailBody(emailBody, studentEmail);
            sendEmailWithAttachment(studentEmail, emailSubject, personalizedBody, file);
            log.info("✅ Envoyé à : {}", studentEmail);
            saveLog(studentEmail, fileName, EmailStatus.SUCCESS, null, sessionId);
            return EmailDto.SendResult.builder()
                    .email(studentEmail).fileName(fileName)
                    .success(true).message("Email envoyé avec succès")
                    .sentAt(LocalDateTime.now()).build();

        } catch (Exception e) {
            String errorMsg = "Erreur : " + e.getMessage();
            log.error("❌ Échec pour {} : {}", studentEmail, e.getMessage());
            saveLog(studentEmail, fileName, EmailStatus.FAILED, errorMsg, sessionId);
            return EmailDto.SendResult.builder()
                    .email(studentEmail).fileName(fileName)
                    .success(false).message(errorMsg)
                    .sentAt(LocalDateTime.now()).build();
        }
    }

    // ---- Extrait l'email depuis le nom du fichier ----
    // Exemple : "amine@gmail.com.pdf" → "amine@gmail.com"
    private String extractEmailFromFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) return null;
        if (!fileName.toLowerCase().endsWith(".pdf")) return null;

        String withoutExtension = fileName.substring(0, fileName.length() - 4);

        if (withoutExtension.contains("@") && withoutExtension.contains(".")) {
            String[] parts = withoutExtension.split("@");
            if (parts.length == 2 && !parts[0].isEmpty() && parts[1].contains(".")) {
                return withoutExtension.toLowerCase().trim();
            }
        }
        return null;
    }

    // ---- Envoie l'email avec le PDF en pièce jointe ----
    private void sendEmailWithAttachment(
            String to, String subject, String body, MultipartFile attachment)
            throws MessagingException, java.io.IOException {

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromEmail, fromName);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(body, false);
        helper.addAttachment(
                attachment.getOriginalFilename(),
                new ByteArrayResource(attachment.getBytes()),
                "application/pdf"
        );
        mailSender.send(message);
    }

    // ---- Personnalise le message avec l'email de l'étudiant ----
    private String personalizeEmailBody(String template, String studentEmail) {
        if (template == null) return "";
        return template
                .replace("{email}", studentEmail)
                .replace("{date}", LocalDateTime.now().toLocalDate().toString());
    }

    // ---- Sauvegarde un log en base de données ----
    private void saveLog(String studentEmail, String fileName,
                         EmailStatus status, String errorMessage, String sessionId) {
        emailLogRepository.save(EmailLog.builder()
                .studentEmail(studentEmail)
                .fileName(fileName)
                .sentAt(LocalDateTime.now())
                .status(status)
                .errorMessage(errorMessage)
                .sessionId(sessionId)
                .build());
    }

    // ---- Récupère tout l'historique ----
    public List<EmailDto.LogResponse> getAllLogs() {
        return emailLogRepository.findAllByOrderBySentAtDesc()
                .stream().map(EmailDto.LogResponse::fromEntity).toList();
    }

    // ---- Statistiques pour le dashboard ----
    public EmailDto.DashboardStats getDashboardStats() {
        long total   = emailLogRepository.count();
        long success = emailLogRepository.countByStatus(EmailStatus.SUCCESS);
        long failed  = emailLogRepository.countByStatus(EmailStatus.FAILED);
        long pending = emailLogRepository.countByStatus(EmailStatus.PENDING);
        double rate  = total > 0 ? Math.round((double) success / total * 1000.0) / 10.0 : 0;

        return EmailDto.DashboardStats.builder()
                .totalSent(total).successCount(success)
                .failureCount(failed).pendingCount(pending)
                .successRate(rate).build();
    }

    // ---- Supprime tout l'historique ----
    public void clearHistory() {
        emailLogRepository.deleteAll();
        log.info("Historique supprimé");
    }
}
