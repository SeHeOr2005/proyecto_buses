package com.sho.ms_security.configurations;

import com.sho.ms_security.models.Permission;
import com.sho.ms_security.models.Role;
import com.sho.ms_security.models.RolePermission;
import com.sho.ms_security.repositories.PermissionRepository;
import com.sho.ms_security.repositories.RolePermissionRepository;
import com.sho.ms_security.repositories.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * HU-ENTR-1-001: Inicializa los roles predeterminados del sistema y los permisos
 * granulares por módulo al arrancar la aplicación.
 * Solo crea los datos si no existen, evitando duplicados.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private RolePermissionRepository rolePermissionRepository;

    @Override
    public void run(String... args) throws Exception {
        initializeDefaultRoles();
        initializeDefaultPermissions();
        initializeAdminRolePermissions();
    }

    /**
     * HU-ENTR-1-001: Roles predeterminados del sistema.
     */
    private void initializeDefaultRoles() {
        List<String[]> defaultRoles = List.of(
                new String[]{"ADMINISTRADOR_SISTEMA",   "Administrador con acceso total al sistema"},
                new String[]{"ADMINISTRADOR_EMPRESA",   "Administrador de empresa de transporte"},
                new String[]{"SUPERVISOR",              "Supervisor de operaciones de transporte"},
                new String[]{"CONDUCTOR",               "Conductor de unidad de transporte"},
                new String[]{"CIUDADANO",               "Usuario ciudadano del sistema de transporte"}
        );

        for (String[] data : defaultRoles) {
            boolean exists = roleRepository.findAll().stream()
                    .anyMatch(r -> r.getName().equals(data[0]));
            if (!exists) {
                Role role = new Role();
                role.setName(data[0]);
                role.setDescription(data[1]);
                roleRepository.save(role);
                System.out.println("Rol creado: " + data[0]);
            }
        }
    }

    /**
     * HU-ENTR-1-001: Permisos granulares por módulo.
     * Formato: [url, method, módulo]
     * Cubre: gestión de usuarios, buses, rutas, programaciones,
     *        reportes, incidentes y mensajes masivos.
     */
    private void initializeDefaultPermissions() {
        List<String[]> defaultPermissions = List.of(
                // ── Gestión de usuarios ──
                new String[]{"/api/users",   "GET",    "usuarios"},
                new String[]{"/api/users",   "POST",   "usuarios"},
                new String[]{"/api/users/?", "GET",    "usuarios"},
                new String[]{"/api/users/?", "PUT",    "usuarios"},
                new String[]{"/api/users/?", "DELETE", "usuarios"},
                new String[]{"/api/users/search", "GET", "usuarios"},

                // ── Gestión de roles ──
                new String[]{"/roles",   "GET",    "roles"},
                new String[]{"/roles",   "POST",   "roles"},
                new String[]{"/roles/?", "GET",    "roles"},
                new String[]{"/roles/?", "PUT",    "roles"},
                new String[]{"/roles/?", "DELETE", "roles"},

                // ── Gestión de permisos ──
                new String[]{"/permissions",   "GET",    "permisos"},
                new String[]{"/permissions",   "POST",   "permisos"},
                new String[]{"/permissions/?", "GET",    "permisos"},
                new String[]{"/permissions/?", "PUT",    "permisos"},
                new String[]{"/permissions/?", "DELETE", "permisos"},

                // ── Gestión de roles-permisos ──
                new String[]{"/role-permission/role/?",               "GET",    "roles"},
                new String[]{"/role-permission/role/?/permission/?",  "POST",   "roles"},
                new String[]{"/role-permission/?",                    "DELETE", "roles"},

                // ── Gestión de usuarios-roles ──
                new String[]{"/user-role",                      "GET",    "usuarios"},
                new String[]{"/user-role/user/?",              "GET",    "usuarios"},
                new String[]{"/user-role/user/?/role/?",       "POST",   "usuarios"},
                new String[]{"/user-role/?",                   "DELETE", "usuarios"},

                // ── Gestión de buses (ms-buses) ──
                new String[]{"/api/buses",   "GET",    "buses"},
                new String[]{"/api/buses",   "POST",   "buses"},
                new String[]{"/api/buses/?", "GET",    "buses"},
                new String[]{"/api/buses/?", "PUT",    "buses"},
                new String[]{"/api/buses/?", "DELETE", "buses"},

                // ── Gestión de rutas (ms-routes) ──
                new String[]{"/api/routes",   "GET",    "rutas"},
                new String[]{"/api/routes",   "POST",   "rutas"},
                new String[]{"/api/routes/?", "GET",    "rutas"},
                new String[]{"/api/routes/?", "PUT",    "rutas"},
                new String[]{"/api/routes/?", "DELETE", "rutas"},

                // ── Gestión de programaciones (ms-schedules) ──
                new String[]{"/api/schedules",   "GET",    "programaciones"},
                new String[]{"/api/schedules",   "POST",   "programaciones"},
                new String[]{"/api/schedules/?", "GET",    "programaciones"},
                new String[]{"/api/schedules/?", "PUT",    "programaciones"},
                new String[]{"/api/schedules/?", "DELETE", "programaciones"},

                // ── Visualización de reportes ──
                new String[]{"/api/reports",   "GET", "reportes"},
                new String[]{"/api/reports/?", "GET", "reportes"},

                // ── Gestión de incidentes ──
                new String[]{"/api/incidents",   "GET",    "incidentes"},
                new String[]{"/api/incidents",   "POST",   "incidentes"},
                new String[]{"/api/incidents/?", "GET",    "incidentes"},
                new String[]{"/api/incidents/?", "PUT",    "incidentes"},
                new String[]{"/api/incidents/?", "DELETE", "incidentes"},

                // ── Envío de mensajes masivos ──
                new String[]{"/api/messages",   "GET",  "mensajes"},
                new String[]{"/api/messages",   "POST", "mensajes"},
                new String[]{"/api/messages/?", "GET",  "mensajes"}
        );

        for (String[] data : defaultPermissions) {
            boolean exists = permissionRepository.findAll().stream()
                    .anyMatch(p -> p.getUrl().equals(data[0]) && p.getMethod().equals(data[1]));
            if (!exists) {
                Permission permission = new Permission();
                permission.setUrl(data[0]);
                permission.setMethod(data[1]);
                permission.setModel(data[2]);
                permissionRepository.save(permission);
            }
        }
        System.out.println("Permisos predeterminados inicializados.");
    }

    /**
     * HU-ENTR-1-001: Asigna automáticamente todos los permisos al rol ADMINISTRADOR_SISTEMA.
     * Solo crea las relaciones que aún no existen.
     */
    private void initializeAdminRolePermissions() {
        Role adminRole = roleRepository.findAll().stream()
                .filter(r -> r.getName().equals("ADMINISTRADOR_SISTEMA"))
                .findFirst()
                .orElse(null);

        if (adminRole == null) {
            System.out.println("Rol ADMINISTRADOR_SISTEMA no encontrado, se omite la asignación de permisos.");
            return;
        }

        List<Permission> allPermissions = permissionRepository.findAll();

        for (Permission permission : allPermissions) {
            RolePermission existing = rolePermissionRepository
                    .getRolePermission(adminRole.getId(), permission.getId());
            if (existing == null) {
                RolePermission rp = new RolePermission(adminRole, permission);
                rolePermissionRepository.save(rp);
            }
        }
        System.out.println("Permisos del ADMINISTRADOR_SISTEMA inicializados: " + allPermissions.size() + " permisos asignados.");
    }
}
