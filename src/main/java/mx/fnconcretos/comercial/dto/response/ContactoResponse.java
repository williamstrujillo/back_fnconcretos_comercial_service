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
public class ContactoResponse {
    private Long id;
    private Long clienteId;
    private String nombre;
    private String telefono;
    private String correo;
    private String cargo;
    private Boolean esGenericoCorporativo;
    private String observaciones;
    private LocalDateTime createdAt;
}
