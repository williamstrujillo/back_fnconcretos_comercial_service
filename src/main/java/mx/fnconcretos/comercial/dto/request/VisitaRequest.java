package mx.fnconcretos.comercial.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VisitaRequest {

    @NotNull(message = "asesorId es obligatorio")
    private Long asesorId;

    private Long obraId;
    private Long clienteId;
    private Long zonaId;

    @NotNull(message = "fechaVisita es obligatoria")
    private LocalDate fechaVisita;

    private String contactoNombre;
    private String contactoTelefono;
    private BigDecimal volumenAproximado;
}
