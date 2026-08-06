package mx.fnconcretos.comercial.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClienteRequest {

    @NotBlank(message = "El numero de cliente es obligatorio")
    private String numeroCliente;

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    /** particular, corporativo */
    private String tipo;

    private String rfc;
    private String telefono;
    private Boolean whatsappDisponible;
    private String correo;
    private Boolean requiereFacturaDefault;
    private Long asesorAsignadoId;
    private BigDecimal limiteCredito;
    private Integer diasCredito;
}
