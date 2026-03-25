package com.sho.ms_security.configurations;

import com.sho.ms_security.interceptors.SecurityInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private SecurityInterceptor securityInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(securityInterceptor)
                .addPathPatterns("/api/**")
                .addPathPatterns("/roles/**")
                .addPathPatterns("/permissions/**")
                .addPathPatterns("/role-permission/**")
                .addPathPatterns("/user-role/**")
                .addPathPatterns("/profiles/**")
                .addPathPatterns("/sessions/**")
                .excludePathPatterns("/api/public/**")
                .excludePathPatterns("/api/users/register")
                .excludePathPatterns("/security/**");
    }
}
