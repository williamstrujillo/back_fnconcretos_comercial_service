package mx.fnconcretos.comercial.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VisitaCheckinRequest {

    @NotNull(message = "latitud es obligatoria")
    private BigDecimal latitud;

    @NotNull(message = "longitud es obligatoria")
    private BigDecimal longitud;

    private String fotoEvidenciaUrl;
    private String contactoNombre;
    private String contactoTelefono;

    /** qr, app */
    private String metodoCheckin;
}
