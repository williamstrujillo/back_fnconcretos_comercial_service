package mx.fnconcretos.comercial.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

/**
 * Cliente hacia auth-service para crear notificaciones (historial + push).
 * No hay descubrimiento de servicios en el proyecto, asi que la URL base es
 * fija por variable de entorno (AUTH_SERVICE_URL), igual que el resto de la
 * configuracion. Una falla aqui nunca debe tumbar el flujo de negocio que la
 * dispara (autorizar un pedido, etc.), solo se loguea.
 */
@Component
public class NotificacionClient {

    private final RestClient restClient;
    private final String claveServicioInterno;

    public NotificacionClient(@Value("${auth-service.base-url}") String authServiceBaseUrl,
                               @Value("${internal.service-key}") String claveServicioInterno) {
        this.restClient = RestClient.builder()
                .baseUrl(authServiceBaseUrl)
                .build();
        this.claveServicioInterno = claveServicioInterno;
    }

    /**
     * bearerToken debe incluir el prefijo "Bearer ". Puede ser null cuando quien dispara la
     * notificacion es un proceso interno sin usuario real detras (ej. el recordatorio de agenda
     * programado) -- la clave de servicio interno basta para autenticar en ese caso.
     */
    public void crear(Long usuarioId, String tipo, String titulo, String mensaje,
                       String referenciaTipo, Long referenciaId, String bearerToken) {
        Map<String, Object> body = new HashMap<>();
        body.put("usuarioId", usuarioId);
        body.put("tipo", tipo);
        body.put("titulo", titulo);
        body.put("mensaje", mensaje);
        body.put("referenciaTipo", referenciaTipo);
        body.put("referenciaId", referenciaId);

        restClient.post()
                .uri("/notificaciones")
                .headers(headers -> {
                    if (bearerToken != null) {
                        headers.set(HttpHeaders.AUTHORIZATION, bearerToken);
                    }
                    headers.set("X-Internal-Service-Key", claveServicioInterno);
                })
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }
}
