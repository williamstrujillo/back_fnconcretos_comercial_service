package mx.fnconcretos.comercial.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContactoRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    private String telefono;
    private String correo;
    private String cargo;
    private Boolean esGenericoCorporativo;
    private String observaciones;
}
