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
@Table(name = "cotizacion_detalle")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CotizacionDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cotizacion_id", nullable = false)
    private Cotizacion cotizacion;

    /** Producto en catalogo-service; solo el id */
    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    @Column(name = "volumen_m3", nullable = false, precision = 10, scale = 2)
    private BigDecimal volumenM3;

    @Column(name = "precio_unitario", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal precioUnitario = BigDecimal.ZERO;

    @Column(name = "precio_total", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal precioTotal = BigDecimal.ZERO;
}
