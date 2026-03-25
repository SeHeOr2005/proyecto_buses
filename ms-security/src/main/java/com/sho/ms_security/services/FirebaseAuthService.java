package com.sho.ms_security.services;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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
                            e
                    );
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
        // Primero intenta variable de entorno GOOGLE_APPLICATION_CREDENTIALS
        String googleAppCreds = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
        if (StringUtils.hasText(googleAppCreds)) {
            LOGGER.debug("Encontrada variable de entorno GOOGLE_APPLICATION_CREDENTIALS: {}", googleAppCreds);
            return googleAppCreds;
        }

        // Luego intenta firebase.credentials.path de properties
        if (StringUtils.hasText(credentialsPath) && !credentialsPath.contains("ruta")) {
            LOGGER.debug("Encontrada propiedad firebase.credentials.path: {}", credentialsPath);
            return credentialsPath;
        }

        LOGGER.debug("No se encontró ruta de credenciales explícita, usará Application Default Credentials");
        return null;
    }
}


