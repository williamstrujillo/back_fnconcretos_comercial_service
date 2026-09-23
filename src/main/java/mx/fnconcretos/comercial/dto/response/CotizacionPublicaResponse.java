package mx.fnconcretos.comercial.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Snapshot congelado de la cotizacion al momento de compartirla -- mapea 1:1 a las props del
 * componente de frontend DocumentoComercialImprimible.jsx, para que la pagina publica pueda
 * pintar exactamente el mismo formato imprimible sin resolver nada en vivo (ver
 * CotizacionCompartidaService). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CotizacionPublicaResponse {

    private String tipoDocumento;
    private String folio;
    private LocalDateTime fecha;
    private String planta;
    private EmpresaInfo empresa;
    private ClienteInfo cliente;
    private EntregaInfo entrega;
    private List<LineaInfo> lineas;
    private BigDecimal totalVolumen;
    private TotalesInfo totales;
    private VendedorInfo vendedor;
    private String observaciones;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EmpresaInfo {
        private String nombre;
        private String razonSocial;
        private String rfc;
        private String telefono;
        private String email;
        private String direccion;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ClienteInfo {
        private String nombre;
        private String telefono;
        private String rfc;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EntregaInfo {
        private String direccion;
        private String elemento;
        private String notas;
        private String fechaHoraServicio;
        private String condicionPago;
        private String bomba;
        private String autorizo;
        private String distanciaKm;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LineaInfo {
        private String codigo;
        private String descripcion;
        private BigDecimal cantidad;
        private String unidad;
        private BigDecimal precioUnitario;
        private BigDecimal importe;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TotalesInfo {
        private BigDecimal subtotal;
        private BigDecimal iva;
        private BigDecimal total;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VendedorInfo {
        private String nombre;
        private String telefono;
    }
}
