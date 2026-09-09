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
import java.time.LocalDateTime;

@Entity
@Table(name = "clientes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_cliente", nullable = false, unique = true, length = 30)
    private String numeroCliente;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    /** particular, corporativo */
    @Column(name = "tipo", nullable = false, length = 20)
    @Builder.Default
    private String tipo = "particular";

    @Column(name = "rfc", length = 20)
    private String rfc;

    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(name = "whatsapp_disponible", nullable = false)
    @Builder.Default
    private Boolean whatsappDisponible = false;

    @Column(name = "correo", length = 120)
    private String correo;

    @Column(name = "requiere_factura_default", nullable = false)
    @Builder.Default
    private Boolean requiereFacturaDefault = false;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "asesor_asignado_id")
    private AsesorComercial asesorAsignado;

    /** asignado (la empresa reparte la cuenta, comision fija), prospectado (el asesor lo consiguio, comision negociada) */
    @Column(name = "origen_captacion", nullable = false, length = 20)
    @Builder.Default
    private String origenCaptacion = "asignado";

    /** % de comision sobre el importe antes de IVA; fijo en 1.00 si origenCaptacion=asignado, negociado (>=1.00) si prospectado */
    @Column(name = "porcentaje_comision", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal porcentajeComision = new BigDecimal("1.00");

    @Column(name = "limite_credito", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal limiteCredito = BigDecimal.ZERO;

    @Column(name = "dias_credito")
    @Builder.Default
    private Integer diasCredito = 0;

    @Column(name = "estatus", nullable = false, length = 20)
    @Builder.Default
    private String estatus = "activo";

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
