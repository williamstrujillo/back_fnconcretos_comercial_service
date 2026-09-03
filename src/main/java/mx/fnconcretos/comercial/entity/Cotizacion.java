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
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cotizaciones")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cotizacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "folio", nullable = false, unique = true, length = 30)
    private String folio;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "obra_id")
    private Obra obra;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "contacto_id")
    private ContactoCliente contacto;

    /** Planta en catalogo-service; solo el id */
    @Column(name = "planta_id")
    private Long plantaId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "asesor_id")
    private AsesorComercial asesor;

    /** Suma del volumen de las lineas tipo 'producto' en cotizacion_detalle; el desglose real vive ahi. */
    @Column(name = "volumen_m3", nullable = false, precision = 10, scale = 2)
    private BigDecimal volumenM3;

    /** directo, bomba */
    @Column(name = "tipo_servicio", nullable = false, length = 20)
    @Builder.Default
    private String tipoServicio = "directo";

    @Column(name = "fecha_suministro_estimada")
    private LocalDate fechaSuministroEstimada;

    /** efectivo, factura */
    @Column(name = "forma_pago", nullable = false, length = 20)
    @Builder.Default
    private String formaPago = "efectivo";

    @Column(name = "requiere_factura", nullable = false)
    @Builder.Default
    private Boolean requiereFactura = false;

    @Column(name = "porcentaje_descuento", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal porcentajeDescuento = BigDecimal.ZERO;

    @Column(name = "precio_unitario", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal precioUnitario = BigDecimal.ZERO;

    @Column(name = "precio_total", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal precioTotal = BigDecimal.ZERO;

    /** negociacion, listo, convertida, cancelada */
    @Column(name = "estatus", nullable = false, length = 20)
    @Builder.Default
    private String estatus = "negociacion";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cotizacion_origen_id")
    private Cotizacion cotizacionOrigen;

    @Column(name = "enviada_whatsapp", nullable = false)
    @Builder.Default
    private Boolean enviadaWhatsapp = false;

    @Column(name = "fecha_envio_whatsapp")
    private LocalDateTime fechaEnvioWhatsapp;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
