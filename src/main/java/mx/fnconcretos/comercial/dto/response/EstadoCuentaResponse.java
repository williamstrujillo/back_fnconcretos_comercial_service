package mx.fnconcretos.comercial.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import mx.fnconcretos.comercial.client.FinanzasClient;

import java.math.BigDecimal;
import java.util.List;

/**
 * Proxy hacia finanzas-service (ClienteEstadoCuentaController, donde vive Pago). Si esa llamada
 * falla, se responde con valores neutros y `disponible=false` en vez de propagar el error -- este
 * dato es informativo, nunca debe bloquear el flujo de cotizar/vender.
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

    // --- Antiguedad de saldos (dashboard de cartera/morosos) ---
    private Integer diasVencidoMax;
    private BigDecimal rango0a30;
    private BigDecimal rango31a60;
    private BigDecimal rango61a90;
    private BigDecimal rango90mas;
    private List<FinanzasClient.PedidoVencidoInfo> pedidosVencidos;
}
