package mx.fnconcretos.comercial.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CotizacionResponse {
    private Long id;
    private String folio;
    private Long clienteId;
    private String clienteNombre;
    private Long obraId;
    private String obraNombre;
    private Long contactoId;
    private Long plantaId;
    private Long asesorId;
    private String asesorNombre;

    /** Suma del volumen de las lineas tipo 'producto' de "productos" (bombeo/flete_vacio no cuentan). */
    private BigDecimal volumenM3;
    /** Desglose real por producto (puede tener 1 o mas lineas). */
    private List<CotizacionItemResponse> productos;
    private String tipoServicio;
    private LocalDate fechaSuministroEstimada;
    private LocalTime horarioEntrega;
    private Long elementoConstructivoId;
    private BigDecimal distanciaKm;
    @Schema(description = "normal | foraneo_1 | foraneo_2 -- determinada por distanciaKm al guardar. "
            + "\"normal\" tambien cuando distanciaKm es null (no se pudo calcular la ruta); en ese caso "
            + "nunca se aplica recargo, para no penalizar al cliente por una limitacion del sistema.")
    private String zonaPrecio;
    @Schema(description = "% de recargo foraneo realmente aplicado (0 si zonaPrecio=normal). Snapshot al "
            + "momento de calcular -- no cambia si los porcentajes/umbrales globales cambian despues.")
    private BigDecimal porcentajeRecargoForaneo;
    private String formaPago;
    private Boolean requiereFactura;
    private BigDecimal porcentajeDescuento;
    private BigDecimal precioUnitario;
    private BigDecimal precioUnitarioConDescuento;
    private BigDecimal subtotal;
    private BigDecimal iva;
    private BigDecimal porcentajeIva;
    private BigDecimal montoTotal;
    private String estatus;
    private Long cotizacionOrigenId;
    private String observaciones;
    private LocalDateTime createdAt;
    private String creadoPorUsuario;
    private String actualizadoPorUsuario;
    private LocalDateTime actualizadoEn;
}
