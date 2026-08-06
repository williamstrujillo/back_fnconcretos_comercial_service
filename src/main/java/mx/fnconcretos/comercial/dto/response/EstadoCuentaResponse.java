package mx.fnconcretos.comercial.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Proxy hacia finanzas-service. Mientras ese servicio no exista, este
 * endpoint responde con valores neutros y `disponible=false` para que el
 * contrato ya quede fijo y comercial-service (autorizacion de credito)
 * pueda integrarlo sin cambios cuando finanzas-service este listo.
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
