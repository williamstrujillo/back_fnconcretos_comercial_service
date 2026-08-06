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
public class DesempenoAsesorResponse {
    private Long asesorId;
    private String asesorNombre;
    private LocalDate desde;
    private LocalDate hasta;
    private long obrasVisitadas;
    private long cotizacionesGeneradas;
    private long cotizacionesConvertidas;
    private double tasaConversionPct;
}
