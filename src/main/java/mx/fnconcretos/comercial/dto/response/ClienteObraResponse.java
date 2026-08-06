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
public class ClienteObraResponse {
    private Long id;
    private Long clienteId;
    private String clienteNombre;
    private Long obraId;
    private String obraNombre;
    private Long contactoId;
    private String contactoNombre;
    private LocalDateTime fechaAsociacion;
}
