package mx.fnconcretos.comercial.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AsesorRequest {

    private Long usuarioId;

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    /** corporativo, asesor */
    private String tipo;

    private BigDecimal porcentajeComisionDefault;
    private Long plantaId;
}
