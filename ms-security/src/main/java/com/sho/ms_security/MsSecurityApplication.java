package com.sho.ms_security;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync // Necesario para el envío de emails asíncronos (HU-ENTR-1-002)
public class MsSecurityApplication {
	public static void main(String[] args) {
		SpringApplication.run(MsSecurityApplication.class, args);
	}
}
