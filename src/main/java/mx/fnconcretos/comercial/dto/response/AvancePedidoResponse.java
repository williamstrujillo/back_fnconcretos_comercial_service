package mx.fnconcretos.comercial.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvancePedidoResponse {
    private Long pedidoId;
    private String folio;
    private String estatusPedido;
    private List<AutorizacionResponse> autorizaciones;

    /** El seguimiento de entrega en ruta (GPS de la olla) lo expone operaciones-service; aqui solo un indicador. */
    private boolean seguimientoEntregaDisponible;
    private LocalDateTime ultimaActualizacion;
}
