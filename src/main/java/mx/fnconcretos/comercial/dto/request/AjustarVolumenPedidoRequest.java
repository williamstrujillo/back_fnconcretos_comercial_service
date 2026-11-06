package mx.fnconcretos.comercial.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Ajuste de m3 de la ultima remision de un pedido -- junta de jefes 2026-11-05: "el cliente decide
 * que en su ultima remision que aun va a salir necesita mas o menos m3". Se aplica sobre
 * PedidoDetalle (nunca sobre la Cotizacion, que es un documento historico congelado una vez
 * compartido), y nunca puede bajar el volumen por debajo de lo ya entregado.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AjustarVolumenPedidoRequest {

    @NotNull(message = "nuevoVolumenM3 es obligatorio")
    @Positive(message = "nuevoVolumenM3 debe ser mayor a 0")
    private BigDecimal nuevoVolumenM3;

    @NotBlank(message = "motivo es obligatorio")
    private String motivo;
}
