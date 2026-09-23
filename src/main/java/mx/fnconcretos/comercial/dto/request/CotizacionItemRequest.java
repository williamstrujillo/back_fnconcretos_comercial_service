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

    /**
     * producto (default), bombeo, flete_vacio, otro. flete_vacio normalmente lo genera el
     * sistema solo (ver CotizacionService.calcularLineas) -- si el request ya trae una linea
     * flete_vacio explicita (el asesor la edito o la borro), se respeta esa en vez de generar
     * una automatica encima.
     */
    private String tipoLinea;

    /** Obligatorio solo si tipoLinea=producto */
    private Long productoId;

    /**
     * Obligatorio si tipoLinea=producto/otro. Para tipoLinea=bombeo es opcional:
     * si se omite, se autocompleta con la suma del volumen de las lineas de producto.
     */
    @DecimalMin(value = "0.0", inclusive = false, message = "El volumen debe ser mayor a 0")
    private BigDecimal volumenM3;

    /**
     * inclusive=true (a diferencia de volumenM3) porque un flete_vacio con tarifa de planta en $0
     * es un caso real (planta sin tarifa de vacio configurada, o Asesor Comercial cuyo precio se
     * fuerza al default -- ver CotizacionService.calcularLineas) y debe poder guardarse tal cual,
     * sin inventar un precio falso solo para pasar la validacion.
     */
    @NotNull(message = "precioUnitario es obligatorio")
    @DecimalMin(value = "0.0", message = "El precio unitario no puede ser negativo")
    private BigDecimal precioUnitario;

    /** etiqueta libre, ej. "Bombeo pluma rentada" */
    private String descripcion;
}
