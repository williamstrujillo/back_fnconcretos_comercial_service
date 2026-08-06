package mx.fnconcretos.comercial.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AsesorResponse {
    private Long id;
    private Long usuarioId;
    private String nombre;
    private String tipo;
    private BigDecimal porcentajeComisionDefault;
    private Long plantaId;
    private String estatus;
    private LocalDateTime createdAt;
}
