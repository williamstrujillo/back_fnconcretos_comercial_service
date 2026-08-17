package mx.fnconcretos.comercial.service;

import lombok.RequiredArgsConstructor;
import mx.fnconcretos.comercial.dto.request.PedidoUpdateRequest;
import mx.fnconcretos.comercial.dto.request.RegistrarEntregaRequest;
import mx.fnconcretos.comercial.dto.response.AutorizacionResponse;
import mx.fnconcretos.comercial.dto.response.AvancePedidoResponse;
import mx.fnconcretos.comercial.dto.response.PedidoResponse;
import mx.fnconcretos.comercial.entity.Pedido;
import mx.fnconcretos.comercial.entity.PedidoAutorizacion;
import mx.fnconcretos.comercial.exception.EstadoInvalidoException;
import mx.fnconcretos.comercial.exception.ResourceNotFoundException;
import mx.fnconcretos.comercial.repository.PedidoAutorizacionRepository;
import mx.fnconcretos.comercial.repository.PedidoRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PedidoService {

    private static final List<String> ESTATUS_PERMITEN_ENTREGA = List.of("autorizado", "programado", "parcial");

    private final PedidoRepository pedidoRepository;
    private final PedidoAutorizacionRepository autorizacionRepository;

    @Transactional(readOnly = true)
    public List<PedidoResponse> listar(Long clienteId, String estatusGeneral) {
        Specification<Pedido> spec = Specification.where(null);

        if (clienteId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("cliente").get("id"), clienteId));
        }
        if (estatusGeneral != null && !estatusGeneral.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("estatusGeneral"), estatusGeneral));
        }

        return pedidoRepository.findAll(spec).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PedidoResponse obtener(Long id) {
        return toResponse(buscarOFallar(id));
    }

    @Transactional
    public PedidoResponse actualizar(Long id, PedidoUpdateRequest request) {
        Pedido pedido = buscarOFallar(id);
        if (!"pendiente_autorizacion_pago".equals(pedido.getEstatusGeneral()) && !"pendiente_autorizacion_logistica".equals(pedido.getEstatusGeneral())) {
            throw new EstadoInvalidoException("Solo se puede editar un pedido mientras esta pendiente de autorizacion");
        }

        if (request.getFechaProgramada() != null) pedido.setFechaProgramada(request.getFechaProgramada());
        if (request.getVolumenSolicitadoM3() != null) {
            pedido.setVolumenSolicitadoM3(request.getVolumenSolicitadoM3());
            pedido.setVolumenPendienteM3(request.getVolumenSolicitadoM3().subtract(pedido.getVolumenEntregadoM3()));
        }
        if (request.getCondicionPago() != null) pedido.setCondicionPago(request.getCondicionPago());
        if (request.getDiasCredito() != null) pedido.setDiasCredito(request.getDiasCredito());

        return toResponse(pedidoRepository.save(pedido));
    }

    /**
     * Llamado desde operaciones-service cuando se firma una remision de entrega.
     * Acumula el volumen entregado y avanza estatusGeneral a parcial/completo.
     */
    @Transactional
    public PedidoResponse registrarEntrega(Long id, RegistrarEntregaRequest request) {
        Pedido pedido = buscarOFallar(id);
        if (!ESTATUS_PERMITEN_ENTREGA.contains(pedido.getEstatusGeneral())) {
            throw new EstadoInvalidoException("El pedido " + id + " no esta en un estatus que permita registrar entregas (estatus actual: "
                    + pedido.getEstatusGeneral() + ")");
        }

        BigDecimal totalEntregado = pedido.getVolumenEntregadoM3().add(request.getMetrosEntregados());
        if (totalEntregado.compareTo(pedido.getVolumenSolicitadoM3()) > 0) {
            totalEntregado = pedido.getVolumenSolicitadoM3();
        }

        pedido.setVolumenEntregadoM3(totalEntregado);
        pedido.setVolumenPendienteM3(pedido.getVolumenSolicitadoM3().subtract(totalEntregado));
        pedido.setEstatusGeneral(totalEntregado.compareTo(pedido.getVolumenSolicitadoM3()) >= 0 ? "completo" : "parcial");

        return toResponse(pedidoRepository.save(pedido));
    }

    @Transactional(readOnly = true)
    public AvancePedidoResponse avance(Long id) {
        Pedido pedido = buscarOFallar(id);
        List<PedidoAutorizacion> autorizaciones = autorizacionRepository.findByPedidoId(id);

        return AvancePedidoResponse.builder()
                .pedidoId(pedido.getId())
                .folio(pedido.getFolio())
                .estatusPedido(pedido.getEstatusGeneral())
                .autorizaciones(autorizaciones.stream().map(this::toAutorizacionResponse).toList())
                .seguimientoEntregaDisponible(false)
                .ultimaActualizacion(LocalDateTime.now())
                .build();
    }

    protected Pedido buscarOFallar(Long id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado: " + id));
    }

    private AutorizacionResponse toAutorizacionResponse(PedidoAutorizacion autorizacion) {
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

    PedidoResponse toResponse(Pedido pedido) {
        return PedidoResponse.builder()
                .id(pedido.getId())
                .folio(pedido.getFolio())
                .cotizacionId(pedido.getCotizacion() != null ? pedido.getCotizacion().getId() : null)
                .clienteId(pedido.getCliente().getId())
                .clienteNombre(pedido.getCliente().getNombre())
                .obraId(pedido.getObra() != null ? pedido.getObra().getId() : null)
                .obraNombre(pedido.getObra() != null ? pedido.getObra().getNombre() : null)
                .plantaId(pedido.getPlantaId())
                .asesorId(pedido.getAsesor() != null ? pedido.getAsesor().getId() : null)
                .asesorNombre(pedido.getAsesor() != null ? pedido.getAsesor().getNombre() : null)
                .productoId(pedido.getProductoId())
                .volumenSolicitadoM3(pedido.getVolumenSolicitadoM3())
                .volumenEntregadoM3(pedido.getVolumenEntregadoM3())
                .volumenPendienteM3(pedido.getVolumenPendienteM3())
                .tipoServicio(pedido.getTipoServicio())
                .fechaProgramada(pedido.getFechaProgramada())
                .condicionPago(pedido.getCondicionPago())
                .diasCredito(pedido.getDiasCredito())
                .estatusPagoAutorizacion(pedido.getEstatusPagoAutorizacion())
                .estatusLogisticaAutorizacion(pedido.getEstatusLogisticaAutorizacion())
                .estatusGeneral(pedido.getEstatusGeneral())
                .motivoRechazo(pedido.getMotivoRechazo())
                .createdAt(pedido.getCreatedAt())
                .build();
    }
}
