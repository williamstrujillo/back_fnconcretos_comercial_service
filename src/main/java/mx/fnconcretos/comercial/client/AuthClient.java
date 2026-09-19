package mx.fnconcretos.comercial.client;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Cliente hacia auth-service para resolver usuarios. No hay descubrimiento de servicios en el
 * proyecto, asi que la URL base es fija por variable de entorno (AUTH_SERVICE_URL), igual que
 * NotificacionClient. Usa el mismo endpoint interno GET /usuarios/por-rol (clave de servicio
 * interno, sin JWT -- pensado para procesos programados sin usuario real detras).
 */
@Component
public class AuthClient {

    private final RestClient restClient;
    private final String claveServicioInterno;

    public AuthClient(@Value("${auth-service.base-url}") String authServiceBaseUrl,
                       @Value("${internal.service-key}") String claveServicioInterno) {
        this.restClient = RestClient.builder()
                .baseUrl(authServiceBaseUrl)
                .build();
        this.claveServicioInterno = claveServicioInterno;
    }

    /** Usuarios activos de un rol (ej. "Direccion"), para notificar a todos a la vez. */
    public List<UsuarioInfo> listarUsuariosPorRol(String nombreRol) {
        List<UsuarioInfo> usuarios = restClient.get()
                .uri("/usuarios/por-rol?nombreRol={nombreRol}", nombreRol)
                .header("X-Internal-Service-Key", claveServicioInterno)
                .retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<List<UsuarioInfo>>() {});
        return usuarios != null ? usuarios : List.of();
    }

    @Data
    public static class UsuarioInfo {
        private Long id;
    }
}
