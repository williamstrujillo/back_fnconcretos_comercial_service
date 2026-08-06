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
import java.time.LocalTime;

@Entity
@Table(name = "visitas_obra")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VisitaObra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "asesor_id", nullable = false)
    private AsesorComercial asesor;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "obra_id")
    private Obra obra;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    /** Zona de cobertura en catalogo-service; solo el id */
    @Column(name = "zona_id")
    private Long zonaId;

    @Column(name = "fecha_visita", nullable = false)
    private LocalDate fechaVisita;

    @Column(name = "hora_checkin")
    private LocalTime horaCheckin;

    @Column(name = "latitud", precision = 10, scale = 7)
    private BigDecimal latitud;

    @Column(name = "longitud", precision = 10, scale = 7)
    private BigDecimal longitud;

    @Column(name = "foto_evidencia_url", length = 255)
    private String fotoEvidenciaUrl;

    @Column(name = "contacto_nombre", length = 120)
    private String contactoNombre;

    @Column(name = "contacto_telefono", length = 20)
    private String contactoTelefono;

    @Column(name = "volumen_aproximado", precision = 10, scale = 2)
    private BigDecimal volumenAproximado;

    /** qr, app */
    @Column(name = "metodo_checkin", length = 20)
    @Builder.Default
    private String metodoCheckin = "app";

    /** asignada, visitada */
    @Column(name = "estatus", nullable = false, length = 20)
    @Builder.Default
    private String estatus = "asignada";

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
