package mx.fnconcretos.comercial.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
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

    /** Obligatorio: de aqui se toma la tarifa de flete por vacio (capacidadReferenciaM3/precioPorM3Vacio). */
    @NotNull(message = "plantaId es obligatorio")
    private Long plantaId;

    private Long asesorId;

    /** Lineas de la cotizacion: productos, bombeo, etc. El flete por vacio se calcula solo, no se manda aqui. */
    @NotEmpty(message = "Debe indicar al menos un producto en 'productos'")
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
}
