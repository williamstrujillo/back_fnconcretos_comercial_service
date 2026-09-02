package mx.fnconcretos.comercial.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CotizacionRequest {

    @NotNull(message = "clienteId es obligatorio")
    private Long clienteId;

    private Long obraId;
    private Long contactoId;
    private Long plantaId;
    private Long asesorId;

    /**
     * productoId/volumenM3 siguen aceptandose para el caso de un solo producto
     * (se ignoran si se manda "productos"). Para mas de un producto en la misma
     * cotizacion (ej. 200 y 250 kg/cm2), usar la lista "productos".
     */
    private Long productoId;

    @DecimalMin(value = "0.0", inclusive = false, message = "El volumen debe ser mayor a 0")
    private BigDecimal volumenM3;

    /** Lista de productos de la cotizacion; si se omite, se usa productoId/volumenM3/precioUnitario como linea unica. */
    @Valid
    private List<CotizacionItemRequest> productos;

    /** directo, bomba */
    private String tipoServicio;

    private LocalDate fechaSuministroEstimada;

    /** efectivo, factura */
    private String formaPago;

    private Boolean requiereFactura;

    @DecimalMin(value = "0.0", message = "El descuento no puede ser negativo")
    private BigDecimal porcentajeDescuento;

    @DecimalMin(value = "0.0", inclusive = false, message = "El precio unitario debe ser mayor a 0")
    private BigDecimal precioUnitario;
}
