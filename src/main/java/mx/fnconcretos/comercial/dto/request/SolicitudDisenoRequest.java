package mx.fnconcretos.comercial.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudDisenoRequest {

    @NotNull(message = "clienteId es obligatorio")
    private Long clienteId;

    private Long obraId;

    @NotBlank(message = "El producto solicitado es obligatorio")
    private String productoSolicitado;

    private String revenimiento;
    private String tamanoAgregado;
    private String caracteristicaEspecial;
    private String aditivosRequeridos;
    private LocalDate fechaDeseada;
    private LocalDate fechaLimiteRespuesta;
    private String insumosRequeridos;
    private String evidenciaUrl;
}
