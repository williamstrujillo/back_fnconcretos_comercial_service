package mx.fnconcretos.comercial.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PedidoResponse {
    private Long id;
    private String folio;
    private Long cotizacionId;
    private Long clienteId;
    private String clienteNombre;
    private Long obraId;
    private String obraNombre;
    private Long plantaId;
    private Long asesorId;
    private String asesorNombre;
    private Long productoId;
    private BigDecimal volumenSolicitadoM3;
    private BigDecimal volumenEntregadoM3;
    private BigDecimal volumenPendienteM3;
    private String tipoServicio;
    private LocalDate fechaProgramada;
    private String condicionPago;
    private Integer diasCredito;
    private String estatusPagoAutorizacion;
    private String estatusLogisticaAutorizacion;
    private String estatusGeneral;
    private String motivoRechazo;
    private LocalDateTime createdAt;
}
