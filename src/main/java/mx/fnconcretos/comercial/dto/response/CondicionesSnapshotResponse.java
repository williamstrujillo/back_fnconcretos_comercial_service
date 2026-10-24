package mx.fnconcretos.comercial.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Copia congelada de la hoja de condiciones al momento de crear/actualizar la cotizacion (ver
 * CotizacionService.resolverCondiciones) -- solo trae las condiciones SELECCIONADAS, ya en orden;
 * editar el catalogo de catalogo-service despues no cambia cotizaciones ya guardadas. Se serializa
 * tal cual a JSON (Cotizacion.condicionesSnapshot / Pedido.condicionesSnapshot) y se deserializa de
 * vuelta en esta misma clase al leer -- reutilizada tanto en CotizacionResponse/PedidoResponse como
 * en el snapshot publico compartido (CotizacionPublicaResponse). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CondicionesSnapshotResponse {
    private Long tipoCondicionId;
    private String nombre;
    private String aviso;
    private Boolean numerarApartados;
    private List<ApartadoSnapshot> apartados;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ApartadoSnapshot {
        private String titulo;
        private String estilo;
        private List<CondicionSnapshot> condiciones;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CondicionSnapshot {
        private String texto;
    }
}
