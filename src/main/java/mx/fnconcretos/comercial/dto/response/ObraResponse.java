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
public class ObraResponse {
    private Long id;
    private String nombre;
    private Long clientePrincipalId;
    private String clientePrincipalNombre;
    private String direccion;
    private String googleMapsLink;
    private BigDecimal latitud;
    private BigDecimal longitud;
    private String ciudad;
    private String colonia;
    private Long plantaId;
    private String estatus;
    private LocalDateTime createdAt;
}
