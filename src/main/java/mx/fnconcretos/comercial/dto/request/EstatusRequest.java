package mx.fnconcretos.comercial.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Compartido por cotizacion-controller y agenda-controller para cambios simples de estatus */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EstatusRequest {

    @NotBlank(message = "El estatus es obligatorio")
    private String estatus;
}
