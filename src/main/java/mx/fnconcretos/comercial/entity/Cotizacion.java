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

    /** Metodo de pago real: efectivo, transferencia, tarjeta_debito, tarjeta_credito (mismo catalogo
     * que Pago.metodoPago en finanzas-service). El tope de descuento ya NO depende de este campo --
     * ver requiereFactura y CotizacionService.validarDescuento. */
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

    /** Suma de las lineas ya con descuento aplicado, SIN iva -- lo que antes era "precioTotal" a
     * secas antes de que existiera el concepto de IVA. */
    @Column(name = "subtotal", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    /** subtotal * porcentajeIva/100, solo si requiereFactura=true; 0 si no. */
    @Column(name = "iva", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal iva = BigDecimal.ZERO;

    /** Tasa realmente usada al calcular iva (snapshot de Planta.porcentajeIva al guardar) -- para
     * que una cotizacion vieja no cambie de total si la tasa de la planta cambia despues. Null si
     * requiereFactura=false (no aplica). */
    @Column(name = "porcentaje_iva", precision = 5, scale = 2)
    private BigDecimal porcentajeIva;

    /** subtotal + iva -- el monto real a cobrar. Se sigue llamando precioTotal por compatibilidad
     * con todo lo que ya lo consume (Pedido.registrarEntrega, estado de cuenta, documentos
     * impresos, etc.) -- antes de que existiera iva, precioTotal YA representaba "el total a
     * cobrar", asi que este cambio es aditivo: para requiereFactura=false, iva=0 y precioTotal no
     * cambia de significado. */
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

    @Column(name = "observaciones", length = 250)
    private String observaciones;

    @Column(name = "enviada_whatsapp", nullable = false)
    @Builder.Default
    private Boolean enviadaWhatsapp = false;

    @Column(name = "fecha_envio_whatsapp")
    private LocalDateTime fechaEnvioWhatsapp;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /** Username (JwtPrincipal.user(), ya viene en el JWT -- sin llamada nueva a auth-service) de
     * quien creo el registro. Null en filas anteriores a este cambio. Inmutable tras crear. */
    @Column(name = "creado_por_usuario", length = 60)
    private String creadoPorUsuario;

    /** Se actualiza en cada crear/actualizar/cambiarEstatus/duplicar. */
    @Column(name = "actualizado_por_usuario", length = 60)
    private String actualizadoPorUsuario;

    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;
}
