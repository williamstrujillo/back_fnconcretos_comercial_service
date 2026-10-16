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
public class CotizacionItemResponse {
    private Long id;
    private String tipoLinea;
    private Long productoId;
    private BigDecimal volumenM3;
    @Schema(description = "Precio de catalogo, antes del recargo foraneo -- igual a precioUnitario en "
            + "lineas que no son tipoLinea=producto (nunca llevan recargo).")
    private BigDecimal precioUnitarioBase;
    @Schema(description = "Precio YA con el recargo foraneo aplicado (si aplica), pero SIN el descuento "
            + "negociado -- precioTotal es quien ya incluye el descuento.")
    private BigDecimal precioUnitario;
    private BigDecimal precioTotal;
    private BigDecimal porcentajeDescuentoLinea;
    private String descripcion;
}
