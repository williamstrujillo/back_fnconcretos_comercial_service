package mx.fnconcretos.comercial.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AutorizacionResponse {
    private Long id;
    private Long pedidoId;
    /** pago, logistica */
    private String tipo;
    private Long autorizadoPor;
    private LocalDateTime fechaAutorizacion;
    /** pendiente, aprobado, rechazado */
    private String resultado;
    private String motivo;
    private LocalDateTime createdAt;
}
