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

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "solicitudes_diseno_especial")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolicitudDisenoEspecial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "obra_id")
    private Obra obra;

    @Column(name = "producto_solicitado", nullable = false, length = 100)
    private String productoSolicitado;

    @Column(name = "revenimiento", length = 20)
    private String revenimiento;

    @Column(name = "tamano_agregado", length = 20)
    private String tamanoAgregado;

    @Column(name = "caracteristica_especial", length = 255)
    private String caracteristicaEspecial;

    @Column(name = "aditivos_requeridos", length = 255)
    private String aditivosRequeridos;

    @Column(name = "fecha_deseada")
    private LocalDate fechaDeseada;

    @Column(name = "fecha_limite_respuesta")
    private LocalDate fechaLimiteRespuesta;

    @Column(name = "insumos_requeridos", length = 255)
    private String insumosRequeridos;

    @Column(name = "evidencia_url", length = 255)
    private String evidenciaUrl;

    /** viable, no_viable */
    @Column(name = "resultado_viabilidad", length = 20)
    private String resultadoViabilidad;

    @Column(name = "estatus", nullable = false, length = 30)
    @Builder.Default
    private String estatus = "recibida";

    /** Usuario de Laboratorio en auth-service; solo el id */
    @Column(name = "responsable_laboratorio_id")
    private Long responsableLaboratorioId;

    @Column(name = "observaciones", length = 255)
    private String observaciones;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
