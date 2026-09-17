package mx.fnconcretos.comercial.client;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Cliente hacia operaciones-service para obtener/crear el token publico de
 * rastreo de un pedido (ver /rastreo/:token en el frontend). Se usa el
 * bearerToken del usuario que dispara la autorizacion — el endpoint de
 * operaciones-service exige auth de empleado, no es publico.
 */
@Component
public class OperacionesClient {

    private final RestClient restClient;

    public OperacionesClient(@Value("${operaciones-service.base-url}") String operacionesServiceBaseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(operacionesServiceBaseUrl)
                .build();
    }

    /** bearerToken debe incluir el prefijo "Bearer ". */
    public String obtenerOCrearTokenSeguimiento(Long pedidoId, String bearerToken) {
        TokenSeguimientoResponse respuesta = restClient.post()
                .uri("/remisiones/pedido/{pedidoId}/token-seguimiento", pedidoId)
                .header(HttpHeaders.AUTHORIZATION, bearerToken)
                .retrieve()
                .body(TokenSeguimientoResponse.class);
        return respuesta != null ? respuesta.getToken() : null;
    }

    @Data
    public static class TokenSeguimientoResponse {
        private Long pedidoId;
        private String token;
    }
}
