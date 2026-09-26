package mx.fnconcretos.comercial.client;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Cliente hacia finanzas-service para resolver el estado de cuenta real de
 * un cliente (Pago vive alla, no aqui). Se usa el bearerToken del usuario
 * que consulta -- el endpoint de finanzas-service exige auth de empleado.
 */
@Component
public class FinanzasClient {

    private final RestClient restClient;

    public FinanzasClient(@Value("${finanzas-service.base-url}") String finanzasServiceBaseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(finanzasServiceBaseUrl)
                .build();
    }

    /** bearerToken debe incluir el prefijo "Bearer ". */
    public EstadoCuentaClienteInfo obtenerEstadoCuenta(Long clienteId, String bearerToken) {
        return restClient.get()
                .uri("/clientes/{id}/estado-cuenta", clienteId)
                .header(HttpHeaders.AUTHORIZATION, bearerToken)
                .retrieve()
                .body(EstadoCuentaClienteInfo.class);
    }

    /** Estado de cuenta de varios clientes en una sola llamada -- bearerToken debe incluir "Bearer ". */
    public List<EstadoCuentaClienteInfo> obtenerEstadoCuentaBatch(List<Long> clienteIds, String bearerToken) {
        String ids = clienteIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        List<EstadoCuentaClienteInfo> infos = restClient.get()
                .uri("/clientes/estado-cuenta/batch?ids={ids}", ids)
                .header(HttpHeaders.AUTHORIZATION, bearerToken)
                .retrieve()
                .body(new ParameterizedTypeReference<List<EstadoCuentaClienteInfo>>() {});
        return infos != null ? infos : List.of();
    }

    @Data
    public static class EstadoCuentaClienteInfo {
        private Long clienteId;
        private boolean disponible;
        private BigDecimal saldoActual;
        private BigDecimal adeudoVencido;
        private BigDecimal anticiposDisponibles;
        private boolean moroso;
        private String mensaje;
    }
}
