package mx.fnconcretos.comercial.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
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
    private String tipoLinea;
    private Long productoId;
    private BigDecimal volumenSolicitadoM3;
    private BigDecimal volumenEntregadoM3;
    private BigDecimal volumenPendienteM3;
    @Schema(description = "Precio de catalogo, antes del recargo foraneo -- copiado de la cotizacion de origen.")
    private BigDecimal precioUnitarioBase;
    private BigDecimal precioUnitario;
    private BigDecimal precioTotal;
    private String descripcion;
}
