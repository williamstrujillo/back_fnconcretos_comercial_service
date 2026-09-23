package mx.fnconcretos.comercial.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.fnconcretos.comercial.client.NotificacionClient;
import mx.fnconcretos.comercial.client.OperacionesClient;
import mx.fnconcretos.comercial.client.WhatsAppClient;
import mx.fnconcretos.comercial.dto.request.AutorizarRequest;
import mx.fnconcretos.comercial.dto.response.AutorizacionResponse;
import mx.fnconcretos.comercial.entity.Cliente;
import mx.fnconcretos.comercial.entity.Pedido;
import mx.fnconcretos.comercial.entity.PedidoAutorizacion;
import mx.fnconcretos.comercial.exception.EstadoInvalidoException;
import mx.fnconcretos.comercial.repository.PedidoAutorizacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutorizacionService {

    private static final String APROBADO = "aprobado";
    private static final String RECHAZADO = "rechazado";
    private static final Locale LOCALE_MX = new Locale("es", "MX");
    private static final DateTimeFormatter FORMATO_FECHA_ENTREGA = DateTimeFormatter.ofPattern("d 'de' MMMM", LOCALE_MX);

    private final PedidoAutorizacionRepository autorizacionRepository;
    private final PedidoService pedidoService;
    private final NotificacionClient notificacionClient;
    private final WhatsAppClient whatsAppClient;
    private final OperacionesClient operacionesClient;
    private final BitacoraService bitacoraService;

    @Transactional(readOnly = true)
    public List<AutorizacionResponse> listarPorPedido(Long pedidoId) {
        return autorizacionRepository.findByPedidoId(pedidoId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public AutorizacionResponse autorizarPago(Long pedidoId, AutorizarRequest request, Long autorizadoPor, String actualizadoPorUsuario, String bearerToken) {
        Pedido pedido = pedidoService.buscarOFallar(pedidoId);
        if (!"pendiente_autorizacion_pago".equals(pedido.getEstatusGeneral())) {
            throw new EstadoInvalidoException("El pedido " + pedidoId + " no esta pendiente de autorizacion de pago (estatus actual: "
                    + pedido.getEstatusGeneral() + ")");
        }

        PedidoAutorizacion autorizacion = registrarAutorizacion(pedido, "pago", request, autorizadoPor);
        pedido.setActualizadoPorUsuario(actualizadoPorUsuario);
        pedido.setActualizadoEn(LocalDateTime.now());

        if (APROBADO.equals(request.getResultado())) {
            pedido.setEstatusPagoAutorizacion(APROBADO);
            pedido.setEstatusGeneral("pendiente_autorizacion_logistica");
            notificarAsesor(pedido, "Pago autorizado",
                    "El pago del pedido " + pedido.getFolio() + " fue autorizado, esta pendiente de autorizacion de logistica.", bearerToken);
            bitacoraService.registrar("pedido", pedido.getId(), "autorizacion_pago", "Autorizo el pago", actualizadoPorUsuario);
        } else {
            pedido.setEstatusPagoAutorizacion(RECHAZADO);
            pedido.setEstatusGeneral("rechazado");
            pedido.setMotivoRechazo(request.getMotivo());
            notificarAsesor(pedido, "Pago rechazado",
                    "El pago del pedido " + pedido.getFolio() + " fue rechazado"
                            + (request.getMotivo() != null ? ": " + request.getMotivo() : "."), bearerToken);
            bitacoraService.registrar("pedido", pedido.getId(), "autorizacion_pago",
                    "Rechazo el pago" + (request.getMotivo() != null ? ": " + request.getMotivo() : ""), actualizadoPorUsuario);
        }

        return toResponse(autorizacion);
    }

    @Transactional
    public AutorizacionResponse autorizarLogistica(Long pedidoId, AutorizarRequest request, Long autorizadoPor, String actualizadoPorUsuario, String bearerToken) {
        Pedido pedido = pedidoService.buscarOFallar(pedidoId);
        if (!"pendiente_autorizacion_logistica".equals(pedido.getEstatusGeneral())) {
            throw new EstadoInvalidoException("El pedido " + pedidoId + " no esta pendiente de autorizacion de logistica (estatus actual: "
                    + pedido.getEstatusGeneral() + ")");
        }

        PedidoAutorizacion autorizacion = registrarAutorizacion(pedido, "logistica", request, autorizadoPor);
        pedido.setActualizadoPorUsuario(actualizadoPorUsuario);
        pedido.setActualizadoEn(LocalDateTime.now());

        if (APROBADO.equals(request.getResultado())) {
            pedido.setEstatusLogisticaAutorizacion(APROBADO);
            pedido.setEstatusGeneral("autorizado");
            notificarAsesor(pedido, "Pedido autorizado",
                    "El pedido " + pedido.getFolio() + " ya quedo autorizado y listo para programar entrega.", bearerToken);
            notificarClienteConfirmacion(pedido, bearerToken);
            bitacoraService.registrar("pedido", pedido.getId(), "autorizacion_logistica", "Autorizo la logistica", actualizadoPorUsuario);
        } else {
            pedido.setEstatusLogisticaAutorizacion(RECHAZADO);
            pedido.setEstatusGeneral("rechazado");
            pedido.setMotivoRechazo(request.getMotivo());
            notificarAsesor(pedido, "Logistica rechazada",
                    "La logistica del pedido " + pedido.getFolio() + " fue rechazada"
                            + (request.getMotivo() != null ? ": " + request.getMotivo() : "."), bearerToken);
            bitacoraService.registrar("pedido", pedido.getId(), "autorizacion_logistica",
                    "Rechazo la logistica" + (request.getMotivo() != null ? ": " + request.getMotivo() : ""), actualizadoPorUsuario);
        }

        return toResponse(autorizacion);
    }

    private void notificarAsesor(Pedido pedido, String titulo, String mensaje, String bearerToken) {
        if (pedido.getAsesor() == null || pedido.getAsesor().getUsuarioId() == null) {
            return;
        }
        try {
            notificacionClient.crear(pedido.getAsesor().getUsuarioId(), "pedido", titulo, mensaje,
                    "pedido", pedido.getId(), bearerToken);
        } catch (Exception e) {
            log.warn("No se pudo notificar al asesor del pedido {}: {}", pedido.getId(), e.getMessage());
        }
    }

    /** Envia el WhatsApp de "pedido confirmado" al cliente, con boton de rastreo. Nunca bloquea la autorizacion. */
    private void notificarClienteConfirmacion(Pedido pedido, String bearerToken) {
        Cliente cliente = pedido.getCliente();
        if (cliente == null || cliente.getTelefono() == null || cliente.getTelefono().isBlank()) {
            return;
        }
        try {
            String token = operacionesClient.obtenerOCrearTokenSeguimiento(pedido.getId(), bearerToken);
            String fechaEntrega = pedido.getFechaProgramada() != null
                    ? pedido.getFechaProgramada().format(FORMATO_FECHA_ENTREGA)
                    : "por confirmar";
            whatsAppClient.enviarPlantilla(cliente.getTelefono(), "pedido_confirmado", "es_MX",
                    List.of(cliente.getNombre(), pedido.getFolio(), fechaEntrega), token);
        } catch (Exception e) {
            log.warn("No se pudo enviar el WhatsApp de pedido confirmado al cliente del pedido {}: {}", pedido.getId(), e.getMessage());
        }
    }

    private PedidoAutorizacion registrarAutorizacion(Pedido pedido, String tipo, AutorizarRequest request, Long autorizadoPor) {
        PedidoAutorizacion autorizacion = autorizacionRepository.findByPedidoIdAndTipo(pedido.getId(), tipo)
                .orElseGet(() -> PedidoAutorizacion.builder().pedido(pedido).tipo(tipo).build());

        autorizacion.setResultado(request.getResultado());
        autorizacion.setMotivo(request.getMotivo());
        autorizacion.setAutorizadoPor(autorizadoPor);
        autorizacion.setFechaAutorizacion(LocalDateTime.now());

        return autorizacionRepository.save(autorizacion);
    }

    private AutorizacionResponse toResponse(PedidoAutorizacion autorizacion) {
        return AutorizacionResponse.builder()
                .id(autorizacion.getId())
                .pedidoId(autorizacion.getPedido().getId())
                .tipo(autorizacion.getTipo())
                .autorizadoPor(autorizacion.getAutorizadoPor())
                .fechaAutorizacion(autorizacion.getFechaAutorizacion())
                .resultado(autorizacion.getResultado())
                .motivo(autorizacion.getMotivo())
                .createdAt(autorizacion.getCreatedAt())
                .build();
    }
}
