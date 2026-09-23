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
public class BitacoraEventoResponse {
    private Long id;
    /** creacion, actualizacion, cambio_estatus, duplicacion, conversion, entrega, autorizacion_pago, autorizacion_logistica */
    private String accion;
    private String descripcion;
    private String usuario;
    private LocalDateTime creadoEn;
}
