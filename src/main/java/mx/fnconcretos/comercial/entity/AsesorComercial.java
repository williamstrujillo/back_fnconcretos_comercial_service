package mx.fnconcretos.comercial.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "asesores_comerciales")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AsesorComercial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Cuenta en auth-service; no se modela como relacion JPA (bounded context distinto) */
    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    /** corporativo, asesor */
    @Column(name = "tipo", nullable = false, length = 20)
    @Builder.Default
    private String tipo = "asesor";

    @Column(name = "porcentaje_comision_default", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal porcentajeComisionDefault = new BigDecimal("2.00");

    /** Planta en catalogo-service; solo el id */
    @Column(name = "planta_id")
    private Long plantaId;

    @Column(name = "estatus", nullable = false, length = 20)
    @Builder.Default
    private String estatus = "activo";

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
