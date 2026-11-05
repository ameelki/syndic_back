package lu.easy.syndic.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/** Envoi d'emails ; sans serveur SMTP configuré, le message est seulement écrit dans les logs. */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final ObjectProvider<JavaMailSender> sender;
    private final String from;
    private final String frontUrl;

    public MailService(ObjectProvider<JavaMailSender> sender, @Value("${app.mail.from}") String from,
                       @Value("${app.front-url}") String frontUrl) {
        this.sender = sender;
        this.from = from;
        this.frontUrl = frontUrl.endsWith("/") ? frontUrl.substring(0, frontUrl.length() - 1) : frontUrl;
    }

    /** Lien vers une page du front, avec l'agence pour que le site prenne sa marque. */
    public String link(String path, String agencySlug) {
        String p = path == null ? "" : path;
        String url = frontUrl + p;
        if (agencySlug != null && !agencySlug.isBlank()) url += (p.contains("?") ? "&" : "?") + "agence=" + agencySlug;
        return url;
    }

    @Async
    public void send(String to, String subject, String text) {
        if (to == null || to.isBlank() || to.endsWith("@invalid.local")) return;
        JavaMailSender s = sender.getIfAvailable();
        if (s == null) {
            log.info("[EMAIL non envoyé : SMTP non configuré] à={} sujet={}\n{}", to, subject, text);
            return;
        }
        try {
            SimpleMailMessage m = new SimpleMailMessage();
            m.setFrom(from);
            m.setTo(to);
            m.setSubject(subject);
            m.setText(text);
            s.send(m);
        } catch (Exception e) {
            log.warn("Échec d'envoi d'email à {} : {}", to, e.getMessage());
        }
    }
}
