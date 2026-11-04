package mx.fnconcretos.comercial.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

/**
 * Cliente hacia auth-service para registrar eventos en la bitacora de auditoria del sistema --
 * mismo patron que NotificacionClient (URL fija por variable de entorno, sin descubrimiento de
 * servicios). A diferencia de NotificacionClient, el try/catch vive AQUI DENTRO (no en cada
 * llamador): un evento de auditoria es un efecto secundario puro, nunca debe bloquear ni afectar
 * la operacion real que lo dispara, y hay demasiados puntos de llamada para repetir el mismo
 * try/catch en cada uno.
 */
@Slf4j
@Component
public class AuditoriaClient {

    private final RestClient restClient;
    private final String claveServicioInterno;

    public AuditoriaClient(@Value("${auth-service.base-url}") String authServiceBaseUrl,
                            @Value("${internal.service-key}") String claveServicioInterno) {
        this.restClient = RestClient.builder()
                .baseUrl(authServiceBaseUrl)
                .build();
        this.claveServicioInterno = claveServicioInterno;
    }

    /** accion: creacion | edicion | borrado. bearerToken debe incluir el prefijo "Bearer ", puede
     * ser null (la clave de servicio interno basta para autenticar). Nunca lanza. */
    public void registrar(Long usuarioId, String usuarioNombre, String accion, String modulo, String detalle,
                           String referenciaTipo, Long referenciaId, String bearerToken) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("usuarioId", usuarioId);
            body.put("usuarioNombre", usuarioNombre);
            body.put("accion", accion);
            body.put("modulo", modulo);
            body.put("detalle", detalle);
            body.put("referenciaTipo", referenciaTipo);
            body.put("referenciaId", referenciaId);

            restClient.post()
                    .uri("/auditoria")
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
        } catch (Exception e) {
            log.warn("No se pudo registrar evento de auditoria ({} / {} / {}): {}", accion, modulo, referenciaId, e.getMessage());
        }
    }
}
