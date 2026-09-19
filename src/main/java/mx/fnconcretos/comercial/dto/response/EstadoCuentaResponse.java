package mx.fnconcretos.comercial.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Proxy hacia finanzas-service (ClienteEstadoCuentaController, donde vive Pago). Si esa llamada
 * falla, se responde con valores neutros y `disponible=false` en vez de propagar el error -- este
 * dato es informativo, nunca debe bloquear el flujo de cotizar/vender.
 * adeudoVencido/moroso quedan siempre en su valor neutro por ahora: no existe todavia una fecha de
 * vencimiento por pedido/factura para calcularlos honestamente (fase 2 pendiente).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstadoCuentaResponse {

    private Long clienteId;

    @Schema(description = "false mientras finanzas-service no este disponible; los montos son 0 por default en ese caso")
    private boolean disponible;

    @Builder.Default
    private BigDecimal saldoActual = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal adeudoVencido = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal anticiposDisponibles = BigDecimal.ZERO;

    @Builder.Default
    private boolean moroso = false;

    private String mensaje;
}
