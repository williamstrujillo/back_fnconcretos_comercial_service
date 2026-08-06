package mx.fnconcretos.comercial.service;

import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
public class AutorizacionService {

    private static final String APROBADO = "aprobado";
    private static final String RECHAZADO = "rechazado";

    private final PedidoAutorizacionRepository autorizacionRepository;
    private final PedidoService pedidoService;

    @Transactional(readOnly = true)
    public List<AutorizacionResponse> listarPorPedido(Long pedidoId) {
        return autorizacionRepository.findByPedidoId(pedidoId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public AutorizacionResponse autorizarPago(Long pedidoId, AutorizarRequest request, Long autorizadoPor) {
        Pedido pedido = pedidoService.buscarOFallar(pedidoId);
        if (!"pendiente_autorizacion_pago".equals(pedido.getEstatusGeneral())) {
            throw new EstadoInvalidoException("El pedido " + pedidoId + " no esta pendiente de autorizacion de pago (estatus actual: "
                    + pedido.getEstatusGeneral() + ")");
        }

        PedidoAutorizacion autorizacion = registrarAutorizacion(pedido, "pago", request, autorizadoPor);

        if (APROBADO.equals(request.getResultado())) {
            pedido.setEstatusPagoAutorizacion(APROBADO);
            pedido.setEstatusGeneral("pendiente_autorizacion_logistica");
        } else {
            pedido.setEstatusPagoAutorizacion(RECHAZADO);
            pedido.setEstatusGeneral("rechazado");
            pedido.setMotivoRechazo(request.getMotivo());
        }

        return toResponse(autorizacion);
    }

    @Transactional
    public AutorizacionResponse autorizarLogistica(Long pedidoId, AutorizarRequest request, Long autorizadoPor) {
        Pedido pedido = pedidoService.buscarOFallar(pedidoId);
        if (!"pendiente_autorizacion_logistica".equals(pedido.getEstatusGeneral())) {
            throw new EstadoInvalidoException("El pedido " + pedidoId + " no esta pendiente de autorizacion de logistica (estatus actual: "
                    + pedido.getEstatusGeneral() + ")");
        }

        PedidoAutorizacion autorizacion = registrarAutorizacion(pedido, "logistica", request, autorizadoPor);

        if (APROBADO.equals(request.getResultado())) {
            pedido.setEstatusLogisticaAutorizacion(APROBADO);
            pedido.setEstatusGeneral("autorizado");
        } else {
            pedido.setEstatusLogisticaAutorizacion(RECHAZADO);
            pedido.setEstatusGeneral("rechazado");
            pedido.setMotivoRechazo(request.getMotivo());
        }

        return toResponse(autorizacion);
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
