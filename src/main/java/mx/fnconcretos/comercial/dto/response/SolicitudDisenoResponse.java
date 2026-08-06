package mx.fnconcretos.comercial.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolicitudDisenoResponse {
    private Long id;
    private Long clienteId;
    private String clienteNombre;
    private Long obraId;
    private String obraNombre;
    private String productoSolicitado;
    private String revenimiento;
    private String tamanoAgregado;
    private String caracteristicaEspecial;
    private String aditivosRequeridos;
    private LocalDate fechaDeseada;
    private LocalDate fechaLimiteRespuesta;
    private String insumosRequeridos;
    private String evidenciaUrl;
    private String resultadoViabilidad;
    private String estatus;
    private Long responsableLaboratorioId;
    private String observaciones;
    private LocalDateTime createdAt;
}
