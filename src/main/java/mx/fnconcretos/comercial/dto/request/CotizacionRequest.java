package mx.fnconcretos.comercial.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
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

    /**
     * Lineas de la cotizacion: productos, bombeo, flete_vacio, etc. El flete por vacio se calcula
     * solo cuando no se incluye aqui explicitamente -- si ya se mando una linea flete_vacio (el
     * asesor la edito o la borro), se respeta esa en vez de regenerarla.
     */
    @NotEmpty(message = "Debe indicar al menos un producto en 'productos'")
    @Valid
    private List<CotizacionItemRequest> productos;

    /** directo, bomba */
    private String tipoServicio;

    private LocalDate fechaSuministroEstimada;

    /** Hora solicitada por el cliente para la entrega. */
    private LocalTime horarioEntrega;

    /** Elemento constructivo (losa, piso, muro, etc.) en catalogo-service. Opcional. */
    private Long elementoConstructivoId;

    /** Metodo de pago real: efectivo, transferencia, tarjeta_debito, tarjeta_credito. */
    private String formaPago;

    /** Determina el tope de descuento permitido (ver CotizacionService.validarDescuento), no solo
     * si se emitira CFDI. */
    private Boolean requiereFactura;

    @DecimalMin(value = "0.0", message = "El descuento no puede ser negativo")
    private BigDecimal porcentajeDescuento;

    @Size(max = 250, message = "Las observaciones no pueden exceder 250 caracteres")
    private String observaciones;
}
