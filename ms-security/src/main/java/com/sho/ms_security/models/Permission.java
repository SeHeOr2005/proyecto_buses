package com.sho.ms_security.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document
public class Permission {
    @Id  // Corregido: faltaba @Id en el proyecto original
    private String id;
    private String url;    // Ej: /api/users, /api/buses
    private String method; // GET, POST, PUT, DELETE
    private String model;  // Módulo: usuarios, buses, rutas, etc.

    public Permission() {}

    public Permission(String id, String url, String method, String model) {
        this.id = id;
        this.url = url;
        this.method = method;
        this.model = model;
    }
}
