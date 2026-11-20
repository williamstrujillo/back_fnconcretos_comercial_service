package mx.fnconcretos.comercial.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Resultado de resolver un link de Google Maps (corto o largo) a coordenadas + direccion/ciudad/
 * colonia. usoApiGoogle indica si hizo falta geocodificar (llamada que se factura) o si todo salio
 * de parsear el link directamente (gratis). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UbicacionResueltaResponse {
    private BigDecimal lat;
    private BigDecimal lng;
    private String direccion;
    private String ciudad;
    private String colonia;
    private boolean usoApiGoogle;
}
