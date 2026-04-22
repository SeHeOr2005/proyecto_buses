package com.sho.ms_security.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@Service
public class FirebaseAuthService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FirebaseAuthService.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Value("${firebase.project-id:}")
    private String projectId;

    @Value("${firebase.credentials.path:}")
    private String credentialsPath;

    private FirebaseAuth firebaseAuth;

    public synchronized FirebaseToken verifyIdToken(String idToken) throws FirebaseAuthException, IOException {
        ensureInitialized();
        return firebaseAuth.verifyIdToken(idToken, true);
    }

    public String getProvider(FirebaseToken token) {
        Object firebaseClaim = token.getClaims().get("firebase");
        if (firebaseClaim instanceof Map<?, ?> claimsMap) {
            Object provider = claimsMap.get("sign_in_provider");
            if (provider != null) {
                return provider.toString();
            }
        }
        return "unknown";
    }

    public void deleteUserByUid(String uid) {
        if (!StringUtils.hasText(uid)) {
            return;
        }
        try {
            ensureInitialized();
            firebaseAuth.deleteUser(uid.trim());
        } catch (FirebaseAuthException ex) {
            LOGGER.warn("No se pudo eliminar usuario Firebase por uid {}: {}", uid, ex.getMessage());
        } catch (Exception ex) {
            LOGGER.warn("No se pudo inicializar Firebase para eliminar uid {}: {}", uid, ex.getMessage());
        }
    }

    public void deleteUserByEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return;
        }
        try {
            ensureInitialized();
            UserRecord userRecord = firebaseAuth.getUserByEmail(email.trim());
            if (userRecord != null && StringUtils.hasText(userRecord.getUid())) {
                firebaseAuth.deleteUser(userRecord.getUid());
            }
        } catch (FirebaseAuthException ex) {
            LOGGER.warn("No se pudo eliminar usuario Firebase por email {}: {}", email, ex.getMessage());
        } catch (Exception ex) {
            LOGGER.warn("No se pudo inicializar Firebase para eliminar email {}: {}", email, ex.getMessage());
        }
    }

    private void ensureInitialized() throws IOException {
        if (firebaseAuth != null) {
            return;
        }

        FirebaseApp app;
        if (!FirebaseApp.getApps().isEmpty()) {
            app = FirebaseApp.getApps().get(0);
        } else {
            FirebaseOptions.Builder optionsBuilder = FirebaseOptions.builder();

            // 1. Prioridad: Intentar leer el JSON directamente desde la variable de entorno
            String firebaseConfigJson = System.getenv("FIREBASE_CONFIG_JSON");

            if (StringUtils.hasText(firebaseConfigJson)) {
                LOGGER.info("Cargando credenciales de Firebase desde variable de entorno FIREBASE_CONFIG_JSON");
                String normalizedJson = normalizeFirebaseConfigJson(firebaseConfigJson);
                try (InputStream targetStream = new ByteArrayInputStream(
                        normalizedJson.getBytes(StandardCharsets.UTF_8))) {
                    optionsBuilder.setCredentials(GoogleCredentials.fromStream(targetStream));
                }
            }
            // 2. Segunda opción: Buscar el archivo físico (útil para desarrollo local)
            else {
                String pathStr = findCredentialsPath();
                if (StringUtils.hasText(pathStr)) {
                    try (InputStream credentialsStream = new FileInputStream(pathStr)) {
                        LOGGER.info("Cargando credenciales de Firebase desde archivo: {}", pathStr);
                        optionsBuilder.setCredentials(GoogleCredentials.fromStream(credentialsStream));
                    }
                } else {
                    // 3. Fallback: Intentar credenciales por defecto del sistema
                    try {
                        LOGGER.info("Intentando usar Application Default Credentials para Firebase");
                        optionsBuilder.setCredentials(GoogleCredentials.getApplicationDefault());
                    } catch (IOException e) {
                        LOGGER.error(
                                "No se encontraron credenciales de Firebase. Configure FIREBASE_CONFIG_JSON en Railway.");
                        throw new IllegalStateException(
                                "Error de configuración: No se encontró el JSON de credenciales de Firebase.", e);
                    }
                }
            }

            if (StringUtils.hasText(projectId)) {
                optionsBuilder.setProjectId(projectId);
            }

            app = FirebaseApp.initializeApp(optionsBuilder.build());
        }

        firebaseAuth = FirebaseAuth.getInstance(app);
    }

    private String normalizeFirebaseConfigJson(String rawConfig) throws IOException {
        JsonNode root = OBJECT_MAPPER.readTree(rawConfig.trim());

        // Railway puede guardar el JSON como string serializado (con comillas
        // externas).
        if (root != null && root.isTextual()) {
            root = OBJECT_MAPPER.readTree(root.asText());
        }

        if (root == null || !root.isObject()) {
            throw new IllegalArgumentException(
                    "FIREBASE_CONFIG_JSON debe ser un objeto JSON con credenciales de service account");
        }

        return OBJECT_MAPPER.writeValueAsString(root);
    }

    private String findCredentialsPath() {
        String[] candidates = new String[] {
                System.getenv("GOOGLE_APPLICATION_CREDENTIALS"),
                System.getenv("FIREBASE_CREDENTIALS_PATH"),
                credentialsPath,
                "confidential/credentials.json"
        };

        for (String candidate : candidates) {
            if (!StringUtils.hasText(candidate) || candidate.contains("ruta")) {
                continue;
            }

            Path path = Paths.get(candidate);
            if (!path.isAbsolute()) {
                path = Paths.get(System.getProperty("user.dir", ".")).resolve(candidate).normalize();
            }

            if (Files.isRegularFile(path)) {
                return path.toString();
            }
        }
        return null;
    }
}