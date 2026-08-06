package mx.fnconcretos.comercial.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AutorizarRequest {

    /** aprobado, rechazado */
    @NotNull(message = "resultado es obligatorio")
    private String resultado;

    private String motivo;
}
