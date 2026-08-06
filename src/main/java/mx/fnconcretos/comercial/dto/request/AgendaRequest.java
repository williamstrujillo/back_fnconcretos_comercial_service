package mx.fnconcretos.comercial.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgendaRequest {

    @NotNull(message = "asesorId es obligatorio")
    private Long asesorId;

    private Long clienteId;
    private Long obraId;
    private Long cotizacionId;
    private Long pedidoId;

    /** llamada, whatsapp, seguimiento, cotizacion, visita, recordatorio */
    @NotNull(message = "tipoActividad es obligatorio")
    private String tipoActividad;

    @NotNull(message = "fechaHora es obligatoria")
    private LocalDateTime fechaHora;

    private Integer minutosRecordatorio;
    private String observaciones;
}
