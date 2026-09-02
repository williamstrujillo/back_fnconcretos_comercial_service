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

import java.math.BigDecimal;

@Entity
@Table(name = "pedido_detalle")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PedidoDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    /** Producto en catalogo-service; solo el id */
    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    @Column(name = "volumen_solicitado_m3", nullable = false, precision = 10, scale = 2)
    private BigDecimal volumenSolicitadoM3;

    @Column(name = "volumen_entregado_m3", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal volumenEntregadoM3 = BigDecimal.ZERO;

    @Column(name = "volumen_pendiente_m3", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal volumenPendienteM3 = BigDecimal.ZERO;

    @Column(name = "precio_unitario", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal precioUnitario = BigDecimal.ZERO;

    @Column(name = "precio_total", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal precioTotal = BigDecimal.ZERO;
}
