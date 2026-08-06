package mx.fnconcretos.comercial.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ObraRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    private Long clientePrincipalId;
    private String direccion;

    @Schema(description = "Link completo de Google Maps recibido por WhatsApp; si trae coordenadas se extraen automaticamente a latitud/longitud")
    private String googleMapsLink;

    @Schema(description = "Solo si no se pudo extraer de googleMapsLink, o para ajustar manualmente")
    private BigDecimal latitud;
    private BigDecimal longitud;

    private String ciudad;
    private String colonia;
    private Long plantaId;
}
