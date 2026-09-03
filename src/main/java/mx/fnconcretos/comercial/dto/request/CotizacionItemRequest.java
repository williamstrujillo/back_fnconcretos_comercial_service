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

    /** producto (default), bombeo, otro. flete_vacio nunca se manda: la genera el sistema. */
    private String tipoLinea;

    /** Obligatorio solo si tipoLinea=producto */
    private Long productoId;

    /**
     * Obligatorio si tipoLinea=producto/otro. Para tipoLinea=bombeo es opcional:
     * si se omite, se autocompleta con la suma del volumen de las lineas de producto.
     */
    @DecimalMin(value = "0.0", inclusive = false, message = "El volumen debe ser mayor a 0")
    private BigDecimal volumenM3;

    @NotNull(message = "precioUnitario es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El precio unitario debe ser mayor a 0")
    private BigDecimal precioUnitario;

    /** etiqueta libre, ej. "Bombeo pluma rentada" */
    private String descripcion;
}
