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
@Table(name = "obras")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Obra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cliente_principal_id")
    private Cliente clientePrincipal;

    @Column(name = "direccion", length = 255)
    private String direccion;

    @Column(name = "google_maps_link", length = 255)
    private String googleMapsLink;

    @Column(name = "latitud", precision = 10, scale = 7)
    private BigDecimal latitud;

    @Column(name = "longitud", precision = 10, scale = 7)
    private BigDecimal longitud;

    @Column(name = "ciudad", length = 100)
    private String ciudad;

    @Column(name = "colonia", length = 100)
    private String colonia;

    /** Planta en catalogo-service; solo el id */
    @Column(name = "planta_id")
    private Long plantaId;

    @Column(name = "estatus", nullable = false, length = 20)
    @Builder.Default
    private String estatus = "activa";

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
