package mx.fnconcretos.comercial.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private String formaPago;
    private Boolean requiereFactura;
    private BigDecimal porcentajeDescuento;
    private BigDecimal precioUnitario;
    private BigDecimal precioUnitarioConDescuento;
    private BigDecimal montoTotal;
    private String estatus;
    private Long cotizacionOrigenId;
    private LocalDateTime createdAt;
}
