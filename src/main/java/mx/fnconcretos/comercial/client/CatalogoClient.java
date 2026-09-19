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

    /** Trae la tarifa de flete por vacio configurada en la planta (tambien trae nombre/empresaId,
     * reutilizado al armar el snapshot publico de cotizacion). bearerToken debe incluir el prefijo "Bearer ". */
    public PlantaTarifas obtenerTarifas(Long plantaId, String bearerToken) {
        return restClient.get()
                .uri("/plantas/{id}", plantaId)
                .header(HttpHeaders.AUTHORIZATION, bearerToken)
                .retrieve()
                .body(PlantaTarifas.class);
    }

    /** Empresa emisora del documento (razon social/RFC/telefono/email/direccion), resuelta via
     * Planta.empresaId. bearerToken debe incluir el prefijo "Bearer ". */
    public EmpresaInfo obtenerEmpresa(Long empresaId, String bearerToken) {
        return restClient.get()
                .uri("/empresas/{id}", empresaId)
                .header(HttpHeaders.AUTHORIZATION, bearerToken)
                .retrieve()
                .body(EmpresaInfo.class);
    }

    /** Nombre del producto, para resolver la descripcion de cada linea del snapshot publico de
     * cotizacion. bearerToken debe incluir el prefijo "Bearer ". */
    public ProductoInfo obtenerProducto(Long productoId, String bearerToken) {
        return restClient.get()
                .uri("/productos/{id}", productoId)
                .header(HttpHeaders.AUTHORIZATION, bearerToken)
                .retrieve()
                .body(ProductoInfo.class);
    }

    @Data
    public static class ProductoInfo {
        private Long id;
        private String nombre;
    }

    @Data
    public static class PlantaTarifas {
        private Long id;
        private String nombre;
        private Long empresaId;
        private BigDecimal capacidadReferenciaM3;
        private BigDecimal precioPorM3Vacio;
        private BigDecimal precioPorM3Bombeo;
    }

    @Data
    public static class EmpresaInfo {
        private Long id;
        private String nombre;
        private String razonSocial;
        private String rfc;
        private String telefono;
        private String email;
        private String direccion;
    }
}
