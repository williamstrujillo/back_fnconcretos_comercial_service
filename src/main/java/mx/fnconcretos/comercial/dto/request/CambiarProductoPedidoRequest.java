package mx.fnconcretos.comercial.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Cambio de producto (ej. resistencia) de una linea del pedido -- junta de jefes 2026-11-05/06:
 * "el cliente requiera algun cambio de resistencia en su pedido". Cada resistencia es un producto
 * distinto en el catalogo (con su propia receta y precio), asi que el cambio es de productoId, no
 * solo de una etiqueta.
 *
 * Solo permitido mientras la linea no tenga nada entregado (PedidoDetalle.volumenEntregadoM3 == 0)
 * -- decision confirmada por el usuario: una vez que salio la primera remision de esa linea, el
 * cambio de producto queda fuera de alcance por ahora.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CambiarProductoPedidoRequest {

    @NotNull(message = "nuevoProductoId es obligatorio")
    private Long nuevoProductoId;

    /** El precio nunca se resuelve solo (mismo criterio que el resto del sistema: lo captura quien
     * atiende el cambio, tomandolo de la lista de precios). */
    @NotNull(message = "nuevoPrecioUnitario es obligatorio")
    @Positive(message = "nuevoPrecioUnitario debe ser mayor a 0")
    private BigDecimal nuevoPrecioUnitario;

    @NotBlank(message = "motivo es obligatorio")
    private String motivo;
}
