package com.certsender;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CertSenderApplication {
    public static void main(String[] args) {
        SpringApplication.run(CertSenderApplication.class, args);
        System.out.println("""
            ╔════════════════════════════════════════╗
            ║   🎓 Cert Sender - Backend démarré     ║
            ║   API disponible sur :8080             ║
            ╚════════════════════════════════════════╝
            """);
    }
}
