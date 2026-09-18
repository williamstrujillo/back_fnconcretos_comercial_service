package mx.fnconcretos.comercial.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Token corto y publico (sin login) para que el cliente final vea/descargue su cotizacion, se
 * comparte por WhatsApp (boton de la plantilla "cotizacion_lista"). Una cotizacion tiene a lo mas
 * un token, generado la primera vez que se comparte y reutilizado despues. El snapshot queda
 * congelado en JSON al momento de compartir -- es un documento de negocio, no debe cambiar si la
 * cotizacion se edita despues (mismo criterio que una factura o cotizacion impresa en papel). */
@Entity
@Table(name = "cotizacion_compartida_token")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CotizacionCompartidaToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cotizacion_id", nullable = false, unique = true)
    private Long cotizacionId;

    @Column(name = "token", nullable = false, unique = true, length = 16)
    private String token;

    @Lob
    @Column(name = "snapshot_json", nullable = false, columnDefinition = "LONGTEXT")
    private String snapshotJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
