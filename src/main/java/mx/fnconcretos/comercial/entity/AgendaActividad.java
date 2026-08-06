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
@Table(name = "agenda_actividades")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgendaActividad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "asesor_id", nullable = false)
    private AsesorComercial asesor;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "obra_id")
    private Obra obra;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cotizacion_id")
    private Cotizacion cotizacion;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "pedido_id")
    private Pedido pedido;

    /** llamada, whatsapp, seguimiento, cotizacion, visita, recordatorio */
    @Column(name = "tipo_actividad", nullable = false, length = 30)
    private String tipoActividad;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "minutos_recordatorio")
    @Builder.Default
    private Integer minutosRecordatorio = 10;

    @Column(name = "estatus", nullable = false, length = 30)
    @Builder.Default
    private String estatus = "pendiente";

    @Column(name = "observaciones", length = 255)
    private String observaciones;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
