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
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "cliente_obra", uniqueConstraints = @UniqueConstraint(columnNames = {"cliente_id", "obra_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClienteObra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "obra_id", nullable = false)
    private Obra obra;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "contacto_id")
    private ContactoCliente contacto;

    @Column(name = "fecha_asociacion", nullable = false)
    @Builder.Default
    private LocalDateTime fechaAsociacion = LocalDateTime.now();
}
