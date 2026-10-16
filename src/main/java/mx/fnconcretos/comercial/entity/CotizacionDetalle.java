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

    /** Producto en catalogo-service; solo aplica cuando tipoLinea=producto */
    @Column(name = "producto_id")
    private Long productoId;

    /** producto, bombeo, flete_vacio, otro */
    @Column(name = "tipo_linea", nullable = false, length = 20)
    @Builder.Default
    private String tipoLinea = "producto";

    @Column(name = "volumen_m3", nullable = false, precision = 10, scale = 2)
    private BigDecimal volumenM3;

    /** Precio unitario de catalogo, ANTES del recargo foraneo -- igual a precioUnitario para
     * lineas que no son tipoLinea=producto (a esas nunca se les aplica recargo foraneo). */
    @Column(name = "precio_unitario_base", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal precioUnitarioBase = BigDecimal.ZERO;

    /** Precio unitario YA con el recargo foraneo aplicado (si tipoLinea=producto y la zona no es
     * "normal"), pero SIN el descuento negociado -- mismo criterio que antes de que existiera el
     * recargo foraneo (precioTotal es quien aplica el descuento, ver
     * CotizacionService.precioConDescuento). */
    @Column(name = "precio_unitario", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal precioUnitario = BigDecimal.ZERO;

    @Column(name = "precio_total", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal precioTotal = BigDecimal.ZERO;

    /** % de descuento EXTRA de esta linea, aparte del descuento general de la cotizacion -- solo
     * aplica a tipoLinea=producto (ver CotizacionService.calcularLineas). Cualquier valor > 0
     * requiere el permiso cotizaciones.aplicar_descuento_especial, igual que salirse del rango del
     * descuento general (ver CotizacionService.validarDescuentosLinea). */
    @Column(name = "porcentaje_descuento_linea", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal porcentajeDescuentoLinea = BigDecimal.ZERO;

    /** etiqueta libre para lineas que no son producto, ej. "Bombeo", "Flete por vacio" */
    @Column(name = "descripcion", length = 120)
    private String descripcion;
}
