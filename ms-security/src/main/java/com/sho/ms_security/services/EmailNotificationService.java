package com.sho.ms_security.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
@Service
public class EmailNotificationService {

    @Value("${notification.service.url:http://localhost:5000}")
    private String notificationServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * HU-ENTR-1-002: Notifica al usuario cuando se le asigna o revoca un rol.
     */
    @Async
    public void sendRoleChangeNotification(String userEmail, String userName,
                                           String roleName, String action) {
        try {
            Map<String, String> body = new HashMap<>();
            body.put("to",       userEmail);
            body.put("name",     userName);
            body.put("roleName", roleName);
            body.put("action",   action);
            restTemplate.postForObject(
                notificationServiceUrl + "/send-role-change", body, Map.class);
        } catch (Exception e) {
            System.err.println("Error al enviar notificación de rol: " + e.getMessage());
        }
    }

    /**
     * HU-ENTR-1-003: Notifica al usuario cuando los permisos de uno de sus roles cambian.
     */
    @Async
    public void sendPermissionChangeNotification(String userEmail, String userName,
                                                 String roleName) {
        try {
            Map<String, String> body = new HashMap<>();
            body.put("to",       userEmail);
            body.put("name",     userName);
            body.put("roleName", roleName);
            restTemplate.postForObject(
                notificationServiceUrl + "/send-permission-change", body, Map.class);
        } catch (Exception e) {
            System.err.println("Error al enviar notificación de permiso: " + e.getMessage());
        }
    }

    /**
     * HU-ENTR-1-007: Envía email de bienvenida al registrarse un nuevo usuario.
     */
    @Async
    public void sendWelcomeEmail(String userEmail, String userName) {
        try {
            String body = "<html><body>"
                + "<p>Estimado/a <strong>" + userName + "</strong>,</p>"
                + "<p>Tu cuenta ha sido creada exitosamente en el Sistema de Transporte SHO.</p>"
                + "<p>Ya puedes iniciar sesión con tu email y contraseña.</p>"
                + "<br><p>Saludos,<br><strong>Equipo de Administración</strong></p>"
                + "</body></html>";

            Map<String, Object> payload = new HashMap<>();
            payload.put("to",      userEmail);
            payload.put("subject", "Bienvenido al Sistema de Transporte SHO");
            payload.put("body",    body);
            payload.put("is_html", true);
            restTemplate.postForObject(notificationServiceUrl + "/send-email", payload, Map.class);
        } catch (Exception e) {
            System.err.println("Error al enviar email de bienvenida: " + e.getMessage());
        }
    }
}
