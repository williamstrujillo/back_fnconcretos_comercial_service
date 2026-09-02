package mx.fnconcretos.comercial.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PedidoItemResponse {
    private Long id;
    private Long productoId;
    private BigDecimal volumenSolicitadoM3;
    private BigDecimal volumenEntregadoM3;
    private BigDecimal volumenPendienteM3;
    private BigDecimal precioUnitario;
    private BigDecimal precioTotal;
}
