package mx.fnconcretos.comercial.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Bitacora de acciones: historial completo (append-only, nunca se edita/borra una fila) de que
 * paso con una cotizacion/pedido a lo largo de su vida -- a diferencia de creadoPorUsuario/
 * actualizadoPorUsuario (que solo guardan el ULTIMO usuario que toco el registro), aqui queda
 * una fila por cada accion, en orden. entidadTipo/entidadId son polimorficos a proposito (una
 * sola tabla sirve para cotizacion y pedido) en vez de una FK real, porque ambas entidades ya
 * comparten este mismo repo/base de datos pero no tiene sentido una tabla de bitacora por cada una.
 */
@Entity
@Table(name = "bitacora_eventos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BitacoraEvento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** cotizacion, pedido */
    @Column(name = "entidad_tipo", nullable = false, length = 20)
    private String entidadTipo;

    @Column(name = "entidad_id", nullable = false)
    private Long entidadId;

    /** creacion, actualizacion, cambio_estatus, duplicacion, conversion, entrega, autorizacion_pago, autorizacion_logistica */
    @Column(name = "accion", nullable = false, length = 40)
    private String accion;

    @Column(name = "descripcion", nullable = false, length = 250)
    private String descripcion;

    /** Username (JwtPrincipal.user()), no id -- igual que creadoPorUsuario/actualizadoPorUsuario. */
    @Column(name = "usuario", length = 60)
    private String usuario;

    @Column(name = "creado_en", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime creadoEn = LocalDateTime.now();
}
