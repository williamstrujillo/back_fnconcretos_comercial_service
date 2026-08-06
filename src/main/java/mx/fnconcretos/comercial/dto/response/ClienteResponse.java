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
public class ClienteResponse {
    private Long id;
    private String numeroCliente;
    private String nombre;
    private String tipo;
    private String rfc;
    private String telefono;
    private Boolean whatsappDisponible;
    private String correo;
    private Boolean requiereFacturaDefault;
    private Long asesorAsignadoId;
    private String asesorAsignadoNombre;
    private BigDecimal limiteCredito;
    private Integer diasCredito;
    private String estatus;
    private LocalDateTime createdAt;
}
