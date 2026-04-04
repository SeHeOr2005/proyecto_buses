package com.sho.ms_security.services;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@Slf4j
public class RecaptchaVerificationService {

    private static final String ENTERPRISE_API_TEMPLATE =
            "https://recaptchaenterprise.googleapis.com/v1/projects/%s/assessments?key=%s";

    @Value("${recaptcha.enabled:true}")
    private boolean recaptchaEnabled;

    @Value("${recaptcha.enterprise.project-id:}")
    private String enterpriseProjectId;

    @Value("${recaptcha.enterprise.api-key:}")
    private String enterpriseApiKey;

    @Value("${recaptcha.enterprise.site-key:}")
    private String enterpriseSiteKey;

    @Value("${recaptcha.min-score:0.5}")
    private double minScore;

    private final RestTemplate restTemplate = new RestTemplate();

    public boolean verifyLoginToken(String token, String remoteIp) {
        return verifyToken(token, "login", remoteIp);
    }

    public boolean verifyToken(String token, String expectedAction, String remoteIp) {
        if (!recaptchaEnabled) {
            return true;
        }

        if (!StringUtils.hasText(token)) {
            log.warn("reCAPTCHA Enterprise rechazado: token vacío");
            return false;
        }

        if (!StringUtils.hasText(enterpriseProjectId)
                || !StringUtils.hasText(enterpriseApiKey)
                || !StringUtils.hasText(enterpriseSiteKey)) {
            log.warn("reCAPTCHA Enterprise rechazado por configuración incompleta");
            return false;
        }

        try {
            String url = String.format(ENTERPRISE_API_TEMPLATE, enterpriseProjectId, enterpriseApiKey);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            EnterpriseEvent event = new EnterpriseEvent();
            event.setToken(token);
            event.setExpectedAction(expectedAction);
            event.setSiteKey(enterpriseSiteKey);
            if (StringUtils.hasText(remoteIp)) {
                event.setUserIpAddress(remoteIp);
            }

            EnterpriseAssessmentRequest body = new EnterpriseAssessmentRequest();
            body.setEvent(event);

            HttpEntity<EnterpriseAssessmentRequest> request = new HttpEntity<>(body, headers);
            EnterpriseAssessmentResponse response = restTemplate.postForObject(
                    url,
                    request,
                    EnterpriseAssessmentResponse.class
            );

            if (response == null || response.getTokenProperties() == null) {
                return false;
            }

            if (!Boolean.TRUE.equals(response.getTokenProperties().getValid())) {
                log.warn("reCAPTCHA Enterprise token inválido. invalidReason={}",
                        response.getTokenProperties().getInvalidReason());
                return false;
            }

            String action = response.getTokenProperties().getAction();
            if (StringUtils.hasText(expectedAction) && !expectedAction.equals(action)) {
                log.warn("reCAPTCHA Enterprise action no coincide. expected={}, actual={}", expectedAction, action);
                return false;
            }

            Double score = response.getRiskAnalysis() != null ? response.getRiskAnalysis().getScore() : null;
            return score != null && score >= minScore;
        } catch (Exception ex) {
            log.error("Error verificando reCAPTCHA Enterprise", ex);
            return false;
        }
    }

    @Data
    public static class EnterpriseAssessmentRequest {
        private EnterpriseEvent event;
    }

    @Data
    public static class EnterpriseEvent {
        private String token;
        private String expectedAction;
        private String siteKey;
        private String userIpAddress;
    }

    @Data
    public static class EnterpriseAssessmentResponse {
        private TokenProperties tokenProperties;
        private RiskAnalysis riskAnalysis;
    }

    @Data
    public static class TokenProperties {
        private Boolean valid;
        private String action;
        private String invalidReason;
    }

    @Data
    public static class RiskAnalysis {
        private Double score;
        private String[] reasons;
    }
}
