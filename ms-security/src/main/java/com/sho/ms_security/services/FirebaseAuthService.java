package com.sho.ms_security.services;

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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@Service
public class FirebaseAuthService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FirebaseAuthService.class);

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

            // Intentar diferentes fuentes de credenciales
            String credentialsPath = findCredentialsPath();

            if (StringUtils.hasText(credentialsPath)) {
                try (InputStream credentialsStream = new FileInputStream(credentialsPath)) {
                    LOGGER.info("Cargando credenciales de Firebase desde archivo: {}", credentialsPath);
                    optionsBuilder.setCredentials(GoogleCredentials.fromStream(credentialsStream));
                } catch (IOException e) {
                    LOGGER.error("Error al cargar credenciales desde archivo: {}", credentialsPath, e);
                    throw e;
                }
            } else {
                try {
                    LOGGER.info("Intentando usar Application Default Credentials para Firebase");
                    optionsBuilder.setCredentials(GoogleCredentials.getApplicationDefault());
                } catch (IOException e) {
                    LOGGER.error("No se encontraron credenciales de Firebase. " +
                            "Configure GOOGLE_APPLICATION_CREDENTIALS o firebase.credentials.path", e);
                    throw new IllegalStateException(
                            "No se encontro credencial de Firebase. Configure GOOGLE_APPLICATION_CREDENTIALS " +
                                    "o defina firebase.credentials.path en application.properties",
                            e);
                }
            }

            if (StringUtils.hasText(projectId)) {
                optionsBuilder.setProjectId(projectId);
            }

            app = FirebaseApp.initializeApp(optionsBuilder.build());
        }

        firebaseAuth = FirebaseAuth.getInstance(app);
    }

    /**
     * Busca la ruta de credenciales en este orden:
     * 1. Variable de entorno GOOGLE_APPLICATION_CREDENTIALS (estándar Google Cloud)
     * 2. Propiedad firebase.credentials.path si está configurada
     * 3. null (cargará Application Default Credentials)
     */
    private String findCredentialsPath() {
        String[] candidates = new String[] {
                System.getenv("GOOGLE_APPLICATION_CREDENTIALS"),
                System.getenv("FIREBASE_CREDENTIALS_PATH"),
                credentialsPath,
                "confidential/credentials.json",
                "../ms-notificaciones/confidential/credentials.json",
                "ms-notificaciones/confidential/credentials.json"
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
                LOGGER.info("Usando credenciales de Firebase desde: {}", path);
                return path.toString();
            }

            LOGGER.debug("Ruta de credenciales no encontrada: {}", path);
        }

        LOGGER.debug("No se encontró ruta de credenciales válida, usará Application Default Credentials");
        return null;
    }
}
