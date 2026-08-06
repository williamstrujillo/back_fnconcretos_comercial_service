package mx.fnconcretos.comercial.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

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
    private Long productoId;

    @NotNull(message = "volumenM3 es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El volumen debe ser mayor a 0")
    private BigDecimal volumenM3;

    /** directo, bomba */
    private String tipoServicio;

    private LocalDate fechaSuministroEstimada;

    /** efectivo, factura */
    private String formaPago;

    private Boolean requiereFactura;

    @DecimalMin(value = "0.0", message = "El descuento no puede ser negativo")
    private BigDecimal porcentajeDescuento;

    @NotNull(message = "precioUnitario es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El precio unitario debe ser mayor a 0")
    private BigDecimal precioUnitario;
}
