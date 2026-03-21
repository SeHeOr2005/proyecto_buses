package com.sho.ms_security.interceptors;

import com.sho.ms_security.services.ValidatorsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Component
public class SecurityInterceptor implements HandlerInterceptor {

    @Autowired
    private ValidatorsService validatorService;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        // Dejar pasar siempre las solicitudes preflight CORS (OPTIONS)
        // El navegador las envía antes de cada request con headers personalizados (Authorization)
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return true;
        }

        boolean allowed = this.validatorService.validationRolePermission(
                request,
                request.getRequestURI(),
                request.getMethod()
        );
        if (!allowed) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"No autorizado: token inválido o sin permisos\"}");
        }
        return allowed;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response,
                           Object handler, ModelAndView modelAndView) throws Exception {}

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) throws Exception {}
}
