package mx.fnconcretos.comercial.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RutaDiariaResponse {
    private Long asesorId;
    private String asesorNombre;
    private LocalDate fecha;
    private long totalActividades;
    private long actividadesCompletadas;
    private long actividadesPendientes;
    private List<AgendaResponse> actividades;
}
