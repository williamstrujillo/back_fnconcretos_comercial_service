package mx.fnconcretos.comercial.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CotizacionItemRequest {

    @NotNull(message = "productoId es obligatorio")
    private Long productoId;

    @NotNull(message = "volumenM3 es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El volumen debe ser mayor a 0")
    private BigDecimal volumenM3;

    @NotNull(message = "precioUnitario es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El precio unitario debe ser mayor a 0")
    private BigDecimal precioUnitario;
}
