package mx.fnconcretos.comercial.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumenVisitasResponse {
    private Long asesorId;
    private String asesorNombre;
    private LocalDate fecha;
    private long visitasRealizadas;
    private long visitasMinimoRequerido;
    private boolean cumpleMinimo;
}
