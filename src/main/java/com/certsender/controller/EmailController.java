package com.certsender.controller;

import com.certsender.dto.EmailDto;
import com.certsender.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/emails")
@RequiredArgsConstructor
@Slf4j
public class EmailController {

    private final EmailService emailService;

    // POST /api/emails/send — Envoie les certificats
    @PostMapping("/send")
    public ResponseEntity<EmailDto.SendSummary> sendCertificates(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("emailSubject") String emailSubject,
            @RequestParam("emailBody") String emailBody) {

        log.info("Requête d'envoi : {} fichiers", files.length);

        if (files == null || files.length == 0)
            return ResponseEntity.badRequest().build();
        if (emailSubject == null || emailSubject.isBlank())
            return ResponseEntity.badRequest().build();

        EmailDto.SendSummary summary = emailService.sendCertificates(files, emailSubject, emailBody);
        return ResponseEntity.ok(summary);
    }

    // GET /api/emails/logs — Historique des envois
    @GetMapping("/logs")
    public ResponseEntity<List<EmailDto.LogResponse>> getLogs() {
        return ResponseEntity.ok(emailService.getAllLogs());
    }

    // GET /api/emails/stats — Statistiques dashboard
    @GetMapping("/stats")
    public ResponseEntity<EmailDto.DashboardStats> getStats() {
        return ResponseEntity.ok(emailService.getDashboardStats());
    }

    // DELETE /api/emails/logs — Supprimer l'historique
    @DeleteMapping("/logs")
    public ResponseEntity<Map<String, String>> clearHistory() {
        emailService.clearHistory();
        return ResponseEntity.ok(Map.of("message", "Historique supprimé"));
    }

    // GET /api/emails/health — Vérification que le backend est vivant
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "message", "Cert Sender API is running"));
    }
}
