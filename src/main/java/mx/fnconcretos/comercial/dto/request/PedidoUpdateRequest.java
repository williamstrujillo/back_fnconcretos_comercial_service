package mx.fnconcretos.comercial.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PedidoUpdateRequest {
    private LocalDate fechaProgramada;
    private BigDecimal volumenSolicitadoM3;
    private String condicionPago;
    private Integer diasCredito;
}
