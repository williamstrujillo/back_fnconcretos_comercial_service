package mx.fnconcretos.comercial.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.fnconcretos.comercial.client.NotificacionClient;
import mx.fnconcretos.comercial.dto.request.AutorizarRequest;
import mx.fnconcretos.comercial.dto.response.AutorizacionResponse;
import mx.fnconcretos.comercial.entity.Pedido;
import mx.fnconcretos.comercial.entity.PedidoAutorizacion;
import mx.fnconcretos.comercial.exception.EstadoInvalidoException;
import mx.fnconcretos.comercial.repository.PedidoAutorizacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutorizacionService {

    private static final String APROBADO = "aprobado";
    private static final String RECHAZADO = "rechazado";

    private final PedidoAutorizacionRepository autorizacionRepository;
    private final PedidoService pedidoService;
    private final NotificacionClient notificacionClient;

    @Transactional(readOnly = true)
    public List<AutorizacionResponse> listarPorPedido(Long pedidoId) {
        return autorizacionRepository.findByPedidoId(pedidoId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public AutorizacionResponse autorizarPago(Long pedidoId, AutorizarRequest request, Long autorizadoPor, String bearerToken) {
        Pedido pedido = pedidoService.buscarOFallar(pedidoId);
        if (!"pendiente_autorizacion_pago".equals(pedido.getEstatusGeneral())) {
            throw new EstadoInvalidoException("El pedido " + pedidoId + " no esta pendiente de autorizacion de pago (estatus actual: "
                    + pedido.getEstatusGeneral() + ")");
        }

        PedidoAutorizacion autorizacion = registrarAutorizacion(pedido, "pago", request, autorizadoPor);

        if (APROBADO.equals(request.getResultado())) {
            pedido.setEstatusPagoAutorizacion(APROBADO);
            pedido.setEstatusGeneral("pendiente_autorizacion_logistica");
            notificarAsesor(pedido, "Pago autorizado",
                    "El pago del pedido " + pedido.getFolio() + " fue autorizado, esta pendiente de autorizacion de logistica.", bearerToken);
        } else {
            pedido.setEstatusPagoAutorizacion(RECHAZADO);
            pedido.setEstatusGeneral("rechazado");
            pedido.setMotivoRechazo(request.getMotivo());
            notificarAsesor(pedido, "Pago rechazado",
                    "El pago del pedido " + pedido.getFolio() + " fue rechazado"
                            + (request.getMotivo() != null ? ": " + request.getMotivo() : "."), bearerToken);
        }

        return toResponse(autorizacion);
    }

    @Transactional
    public AutorizacionResponse autorizarLogistica(Long pedidoId, AutorizarRequest request, Long autorizadoPor, String bearerToken) {
        Pedido pedido = pedidoService.buscarOFallar(pedidoId);
        if (!"pendiente_autorizacion_logistica".equals(pedido.getEstatusGeneral())) {
            throw new EstadoInvalidoException("El pedido " + pedidoId + " no esta pendiente de autorizacion de logistica (estatus actual: "
                    + pedido.getEstatusGeneral() + ")");
        }

        PedidoAutorizacion autorizacion = registrarAutorizacion(pedido, "logistica", request, autorizadoPor);

        if (APROBADO.equals(request.getResultado())) {
            pedido.setEstatusLogisticaAutorizacion(APROBADO);
            pedido.setEstatusGeneral("autorizado");
            notificarAsesor(pedido, "Pedido autorizado",
                    "El pedido " + pedido.getFolio() + " ya quedo autorizado y listo para programar entrega.", bearerToken);
        } else {
            pedido.setEstatusLogisticaAutorizacion(RECHAZADO);
            pedido.setEstatusGeneral("rechazado");
            pedido.setMotivoRechazo(request.getMotivo());
            notificarAsesor(pedido, "Logistica rechazada",
                    "La logistica del pedido " + pedido.getFolio() + " fue rechazada"
                            + (request.getMotivo() != null ? ": " + request.getMotivo() : "."), bearerToken);
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
