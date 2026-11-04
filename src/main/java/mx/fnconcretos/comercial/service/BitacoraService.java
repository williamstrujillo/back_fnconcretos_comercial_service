package mx.fnconcretos.comercial.service;

import lombok.RequiredArgsConstructor;
import mx.fnconcretos.comercial.client.AuditoriaClient;
import mx.fnconcretos.comercial.dto.response.BitacoraEventoResponse;
import mx.fnconcretos.comercial.entity.BitacoraEvento;
import mx.fnconcretos.comercial.repository.BitacoraEventoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BitacoraService {

    private final BitacoraEventoRepository bitacoraEventoRepository;
    private final AuditoriaClient auditoriaClient;

    /** "creacion" tal cual coincide con la bitacora de auditoria del sistema; todo lo demas que esta
     * bitacora local ya registra (cambio de estatus, duplicacion, conversion, entrega, autorizacion
     * de pago/logistica) es una modificacion sobre un registro existente -- no hay "borrado" porque
     * cotizaciones/pedidos nunca se eliminan fisicamente. */
    private static final Map<String, String> ACCION_SISTEMA = Map.of("creacion", "creacion");

    private static final Map<String, String> MODULO_POR_ENTIDAD = Map.of(
            "cotizacion", "Cotizaciones",
            "pedido", "Pedidos");

    @Transactional
    public void registrar(String entidadTipo, Long entidadId, String accion, String descripcion, String usuario) {
        bitacoraEventoRepository.save(BitacoraEvento.builder()
                .entidadTipo(entidadTipo)
                .entidadId(entidadId)
                .accion(accion)
                .descripcion(descripcion)
                .usuario(usuario)
                .build());

        // Tambien alimenta la bitacora de auditoria del sistema (auth-service) -- un solo puente
        // aqui evita tocar cada uno de los ~12 puntos que ya llaman a este metodo. usuarioId se deja
        // null: esta bitacora local solo guarda el username (igual que creadoPorUsuario/
        // actualizadoPorUsuario en toda la app), el nombre ya identifica al actor.
        auditoriaClient.registrar(null, usuario != null ? usuario : "Sistema",
                ACCION_SISTEMA.getOrDefault(accion, "edicion"),
                MODULO_POR_ENTIDAD.getOrDefault(entidadTipo, entidadTipo),
                descripcion, entidadTipo, entidadId, null);
    }

    @Transactional(readOnly = true)
    public List<BitacoraEventoResponse> listar(String entidadTipo, Long entidadId) {
        return bitacoraEventoRepository.findByEntidadTipoAndEntidadIdOrderByCreadoEnDesc(entidadTipo, entidadId).stream()
                .map(e -> BitacoraEventoResponse.builder()
                        .id(e.getId())
                        .accion(e.getAccion())
                        .descripcion(e.getDescripcion())
                        .usuario(e.getUsuario())
                        .creadoEn(e.getCreadoEn())
                        .build())
                .toList();
    }
}
