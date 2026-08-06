package mx.fnconcretos.comercial.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VisitaResponse {
    private Long id;
    private Long asesorId;
    private String asesorNombre;
    private Long obraId;
    private String obraNombre;
    private Long clienteId;
    private String clienteNombre;
    private Long zonaId;
    private LocalDate fechaVisita;
    private LocalTime horaCheckin;
    private BigDecimal latitud;
    private BigDecimal longitud;
    private String fotoEvidenciaUrl;
    private String contactoNombre;
    private String contactoTelefono;
    private BigDecimal volumenAproximado;
    private String metodoCheckin;
    private String estatus;
    private LocalDateTime createdAt;
}
