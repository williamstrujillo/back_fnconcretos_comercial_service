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

    public NotificacionClient(@Value("${auth-service.base-url}") String authServiceBaseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(authServiceBaseUrl)
                .build();
    }

    /** bearerToken debe incluir el prefijo "Bearer ". */
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
                .header(HttpHeaders.AUTHORIZATION, bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }
}
