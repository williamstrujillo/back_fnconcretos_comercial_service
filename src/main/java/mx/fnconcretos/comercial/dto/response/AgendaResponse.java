package mx.fnconcretos.comercial.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgendaResponse {
    private Long id;
    private Long asesorId;
    private String asesorNombre;
    private Long clienteId;
    private String clienteNombre;
    private Long obraId;
    private String obraNombre;
    private Long cotizacionId;
    private Long pedidoId;
    private String tipoActividad;
    private LocalDateTime fechaHora;
    private Integer minutosRecordatorio;
    private String estatus;
    private String observaciones;
    private LocalDateTime createdAt;
}
