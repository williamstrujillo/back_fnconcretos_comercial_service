package mx.fnconcretos.comercial.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VincularClienteRequest {

    @NotNull(message = "clienteId es obligatorio")
    private Long clienteId;

    private Long contactoId;
}
