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
public class PedidoResponse {
    private Long id;
    private String folio;
    private Long cotizacionId;
    private Long clienteId;
    private String clienteNombre;
    private Long obraId;
    private String obraNombre;
    private Long plantaId;
    private Long asesorId;
    private String asesorNombre;

    /** Agregados de las lineas tipo 'producto' de "productos" (bombeo/flete_vacio no cuentan). */
    private BigDecimal volumenSolicitadoM3;
    private BigDecimal volumenEntregadoM3;
    private BigDecimal volumenPendienteM3;
    /** Desglose real por producto (puede tener 1 o mas lineas). */
    private List<PedidoItemResponse> productos;
    private String tipoServicio;
    private LocalDate fechaProgramada;
    private String condicionPago;
    private Integer diasCredito;
    /** Metodo con el que el cliente va a pagar (efectivo, transferencia, tarjeta_debito,
     * tarjeta_credito) -- tomado de la cotizacion de origen, no un campo propio del pedido. */
    private String formaPago;
    /** Suma de precioTotal de todas las lineas (producto + bombeo + flete_vacio + iva). */
    private java.math.BigDecimal montoTotal;
    private String estatusPagoAutorizacion;
    private String estatusLogisticaAutorizacion;
    private String estatusGeneral;
    private String motivoRechazo;
    private LocalDateTime createdAt;
    private String creadoPorUsuario;
    private String actualizadoPorUsuario;
    private LocalDateTime actualizadoEn;
}
