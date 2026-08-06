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
@Table(name = "pedidos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "folio", nullable = false, unique = true, length = 30)
    private String folio;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cotizacion_id")
    private Cotizacion cotizacion;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "obra_id")
    private Obra obra;

    /** Planta en catalogo-service; solo el id */
    @Column(name = "planta_id")
    private Long plantaId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "asesor_id")
    private AsesorComercial asesor;

    /** Producto en catalogo-service; solo el id */
    @Column(name = "producto_id")
    private Long productoId;

    @Column(name = "volumen_solicitado_m3", nullable = false, precision = 10, scale = 2)
    private BigDecimal volumenSolicitadoM3;

    @Column(name = "volumen_entregado_m3", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal volumenEntregadoM3 = BigDecimal.ZERO;

    @Column(name = "volumen_pendiente_m3", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal volumenPendienteM3 = BigDecimal.ZERO;

    /** directo, bomba */
    @Column(name = "tipo_servicio", nullable = false, length = 20)
    @Builder.Default
    private String tipoServicio = "directo";

    @Column(name = "fecha_programada")
    private LocalDate fechaProgramada;

    /** liquidado, anticipo, credito, liquidar_obra */
    @Column(name = "condicion_pago", nullable = false, length = 20)
    @Builder.Default
    private String condicionPago = "liquidado";

    @Column(name = "dias_credito")
    private Integer diasCredito;

    @Column(name = "estatus_pago_autorizacion", nullable = false, length = 30)
    @Builder.Default
    private String estatusPagoAutorizacion = "pendiente";

    @Column(name = "estatus_logistica_autorizacion", nullable = false, length = 30)
    @Builder.Default
    private String estatusLogisticaAutorizacion = "pendiente";

    @Column(name = "estatus_general", nullable = false, length = 40)
    @Builder.Default
    private String estatusGeneral = "pendiente_autorizacion_pago";

    @Column(name = "motivo_rechazo", length = 255)
    private String motivoRechazo;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
