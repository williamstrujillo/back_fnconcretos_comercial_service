package mx.fnconcretos.comercial.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "pedido_autorizaciones")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PedidoAutorizacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    /** pago, logistica */
    @Column(name = "tipo", nullable = false, length = 20)
    private String tipo;

    /** Usuario en auth-service; solo el id */
    @Column(name = "autorizado_por")
    private Long autorizadoPor;

    @Column(name = "fecha_autorizacion")
    private LocalDateTime fechaAutorizacion;

    /** pendiente, aprobado, rechazado */
    @Column(name = "resultado", nullable = false, length = 20)
    @Builder.Default
    private String resultado = "pendiente";

    @Column(name = "motivo", length = 255)
    private String motivo;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
