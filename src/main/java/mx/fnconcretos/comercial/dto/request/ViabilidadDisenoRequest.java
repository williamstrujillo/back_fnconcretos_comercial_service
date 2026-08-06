package mx.fnconcretos.comercial.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ViabilidadDisenoRequest {

    /** viable, no_viable */
    @NotNull(message = "resultadoViabilidad es obligatorio")
    private String resultadoViabilidad;

    private String observaciones;
}
