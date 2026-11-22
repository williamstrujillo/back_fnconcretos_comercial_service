package mx.fnconcretos.comercial.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
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

    /** clienteId o clienteNombreLibre: uno de los dos es obligatorio (validado en el service, no
     * aqui, porque la regla es "uno u otro" y Bean Validation no expresa eso limpio entre 2 campos). */
    private Long clienteId;

    /** Solo para cotizacion simple (sin clienteId) -- nombre del prospecto en texto libre. */
    @Size(max = 150, message = "clienteNombreLibre debe tener maximo 150 caracteres")
    private String clienteNombreLibre;

    private Long obraId;

    /** Solo cuando no se manda obraId -- nombre de la obra en texto libre. */
    @Size(max = 150, message = "obraNombreLibre debe tener maximo 150 caracteres")
    private String obraNombreLibre;

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

    /** % de MARGEN (no descuento directo, ver CotizacionService.precioConMargen): negativo rebaja
     * el precio, positivo lo aumenta (markup). El limite superior evita que precio_unitario /
     * (1 - margen/100) quede indefinido o negativo; el rango habilitado sin permiso especial
     * (-6%..0% sin factura, -11%..-8% con factura) se valida en CotizacionService.validarDescuento. */
    @DecimalMax(value = "99.99", message = "El margen no puede ser mayor o igual a 100%")
    private BigDecimal porcentajeDescuento;

    @Size(max = 250, message = "Las observaciones no pueden exceder 250 caracteres")
    private String observaciones;

    /** Tipo de condicion comercial en catalogo-service. Opcional -- si no viene, se usa el tipo
     * predeterminado activo; si tampoco hay uno, la cotizacion queda sin hoja de condiciones. */
    private Long tipoCondicionId;
}
