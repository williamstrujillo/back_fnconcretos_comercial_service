package mx.fnconcretos.comercial.client;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

/**
 * Cliente hacia catalogo-service. No hay descubrimiento de servicios en el
 * proyecto, asi que la URL base es fija por variable de entorno
 * (CATALOGO_SERVICE_URL), igual que ComercialClient en operaciones-service.
 */
@org.springframework.stereotype.Component
public class CatalogoClient {

    private final RestClient restClient;

    public CatalogoClient(@Value("${catalogo-service.base-url}") String catalogoServiceBaseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(catalogoServiceBaseUrl)
                .build();
    }

    /** Trae la tarifa de flete por vacio configurada en la planta. bearerToken debe incluir el prefijo "Bearer ". */
    public PlantaTarifas obtenerTarifas(Long plantaId, String bearerToken) {
        return restClient.get()
                .uri("/plantas/{id}", plantaId)
                .header(HttpHeaders.AUTHORIZATION, bearerToken)
                .retrieve()
                .body(PlantaTarifas.class);
    }

    @Data
    public static class PlantaTarifas {
        private Long id;
        private BigDecimal capacidadReferenciaM3;
        private BigDecimal precioPorM3Vacio;
    }
}
