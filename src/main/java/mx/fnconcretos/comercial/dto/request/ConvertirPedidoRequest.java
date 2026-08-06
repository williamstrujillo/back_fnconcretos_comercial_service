package mx.fnconcretos.comercial.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConvertirPedidoRequest {

    @NotNull(message = "fechaProgramada es obligatoria")
    private LocalDate fechaProgramada;

    /** liquidado, anticipo, credito, liquidar_obra */
    @NotNull(message = "condicionPago es obligatoria")
    private String condicionPago;

    private Integer diasCredito;
}
