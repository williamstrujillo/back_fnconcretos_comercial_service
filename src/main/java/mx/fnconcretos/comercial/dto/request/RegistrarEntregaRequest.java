package mx.fnconcretos.comercial.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistrarEntregaRequest {

    /** Remision en operaciones-service; solo el id, para trazabilidad */
    private Long remisionId;

    @NotNull(message = "metrosEntregados es obligatorio")
    private BigDecimal metrosEntregados;

    /** Linea de pedido_detalle que esta entrega surte; null si el pedido no tiene desglose (legado). */
    private Long pedidoDetalleId;

    /** Producto entregado; se usa para ubicar la linea si no se manda pedidoDetalleId. */
    private Long productoId;
}
