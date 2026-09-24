package mx.fnconcretos.comercial.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.fnconcretos.comercial.client.AuthClient;
import mx.fnconcretos.comercial.client.NotificacionClient;
import mx.fnconcretos.comercial.client.OperacionesClient;
import mx.fnconcretos.comercial.client.WhatsAppClient;
import mx.fnconcretos.comercial.dto.request.PedidoUpdateRequest;
import mx.fnconcretos.comercial.dto.request.RegistrarEntregaRequest;
import mx.fnconcretos.comercial.dto.response.AutorizacionResponse;
import mx.fnconcretos.comercial.dto.response.AvancePedidoResponse;
import mx.fnconcretos.comercial.dto.response.PedidoItemResponse;
import mx.fnconcretos.comercial.dto.response.PedidoResponse;
import mx.fnconcretos.comercial.dto.response.WhatsAppEnvioResponse;
import mx.fnconcretos.comercial.entity.Pedido;
import mx.fnconcretos.comercial.entity.PedidoAutorizacion;
import mx.fnconcretos.comercial.entity.PedidoDetalle;
import mx.fnconcretos.comercial.exception.EstadoInvalidoException;
import mx.fnconcretos.comercial.exception.ResourceNotFoundException;
import mx.fnconcretos.comercial.repository.PedidoAutorizacionRepository;
import mx.fnconcretos.comercial.repository.PedidoDetalleRepository;
import mx.fnconcretos.comercial.repository.PedidoRepository;
import mx.fnconcretos.comercial.security.JwtPrincipal;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PedidoService {

    private static final List<String> ESTATUS_PERMITEN_ENTREGA = List.of("autorizado", "programado", "parcial");

    /** Dias antes del vencimiento del credito en que se dispara el primer (y unico) aviso. */
    private static final int DIAS_AVISO_VENCIMIENTO_CREDITO = 3;
    private static final String ROL_DIRECCION = "Direccion";

    private final PedidoRepository pedidoRepository;
    private final PedidoDetalleRepository pedidoDetalleRepository;
    private final PedidoAutorizacionRepository autorizacionRepository;
    private final NotificacionClient notificacionClient;
    private final AuthClient authClient;
    private final WhatsAppClient whatsAppClient;
    private final OperacionesClient operacionesClient;
    private final BitacoraService bitacoraService;

    @Transactional(readOnly = true)
    public List<PedidoResponse> listar(Long clienteId, String estatusGeneral, String q, Long plantaId) {
        Specification<Pedido> spec = Specification.where(null);

        if (clienteId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("cliente").get("id"), clienteId));
        }
        if (plantaId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("plantaId"), plantaId));
        }
        if (estatusGeneral != null && !estatusGeneral.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("estatusGeneral"), estatusGeneral));
        }
        if (q != null && !q.isBlank()) {
            String like = "%" + q.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("folio")), like));
        }

        return pedidoRepository.findAll(spec).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PedidoResponse obtener(Long id) {
        return toResponse(buscarOFallar(id));
    }

    /**
     * Envia (o reenvia) por WhatsApp el link de seguimiento del pedido, via la API de WhatsApp
     * Business (plantilla aprobada "pedido_seguimiento") -- reemplaza el link manual wa.me que
     * usaba el boton "Compartir seguimiento" del frontend. Disparo manual (el asesor decide el
     * momento), a diferencia de AutorizacionService.notificarClienteConfirmacion (automatico al
     * autorizar logistica, plantilla distinta "pedido_confirmado") -- por eso aqui SI se propaga el
     * error al llamador en vez de solo loguearlo, para que el frontend le muestre al asesor si de
     * verdad se envio o no.
     */
    @Transactional
    public WhatsAppEnvioResponse enviarWhatsAppSeguimiento(Long id, String bearerToken) {
        Pedido pedido = buscarOFallar(id);
        if (pedido.getCliente() == null || pedido.getCliente().getTelefono() == null || pedido.getCliente().getTelefono().isBlank()) {
            throw new EstadoInvalidoException("El cliente no tiene telefono registrado");
        }

        String token = operacionesClient.obtenerOCrearTokenSeguimiento(id, bearerToken);
        whatsAppClient.enviarPlantilla(pedido.getCliente().getTelefono(), "pedido_seguimiento", "es_MX",
                List.of(pedido.getCliente().getNombre(), pedido.getFolio()), token);

        return WhatsAppEnvioResponse.builder().enviado(true).telefono(pedido.getCliente().getTelefono()).build();
    }

    @Transactional
    public PedidoResponse actualizar(Long id, PedidoUpdateRequest request, JwtPrincipal principal) {
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

        String usuario = principal != null ? principal.user() : null;
        pedido.setActualizadoPorUsuario(usuario);
        pedido.setActualizadoEn(LocalDateTime.now());

        Pedido guardado = pedidoRepository.save(pedido);
        bitacoraService.registrar("pedido", guardado.getId(), "actualizacion",
                "Actualizo los datos del pedido", usuario);

        return toResponse(guardado);
    }

    /**
     * Llamado desde operaciones-service cuando se firma una remision de entrega.
     * Acumula el volumen entregado y avanza estatusGeneral a parcial/completo.
     */
    @Transactional
    public PedidoResponse registrarEntrega(Long id, RegistrarEntregaRequest request, String bearerToken) {
        Pedido pedido = buscarOFallar(id);
        if (!ESTATUS_PERMITEN_ENTREGA.contains(pedido.getEstatusGeneral())) {
            throw new EstadoInvalidoException("El pedido " + id + " no esta en un estatus que permita registrar entregas (estatus actual: "
                    + pedido.getEstatusGeneral() + ")");
        }

        List<PedidoDetalle> lineasProducto = pedidoDetalleRepository.findByPedidoId(id).stream()
                .filter(l -> "producto".equals(l.getTipoLinea()))
                .toList();
        if (lineasProducto.isEmpty()) {
            // Pedido legado (sin desglose por producto): igual que antes, se acumula solo a nivel encabezado.
            acumularEntregaEnEncabezado(pedido, request.getMetrosEntregados());
        } else {
            PedidoDetalle linea = resolverLinea(lineasProducto, request);
            BigDecimal totalLinea = linea.getVolumenEntregadoM3().add(request.getMetrosEntregados());
            if (totalLinea.compareTo(linea.getVolumenSolicitadoM3()) > 0) {
                totalLinea = linea.getVolumenSolicitadoM3();
            }
            linea.setVolumenEntregadoM3(totalLinea);
            linea.setVolumenPendienteM3(linea.getVolumenSolicitadoM3().subtract(totalLinea));
            pedidoDetalleRepository.save(linea);

            BigDecimal sumEntregado = lineasProducto.stream().map(PedidoDetalle::getVolumenEntregadoM3).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal sumPendiente = lineasProducto.stream().map(PedidoDetalle::getVolumenPendienteM3).reduce(BigDecimal.ZERO, BigDecimal::add);
            boolean todasLasLineasCompletas = lineasProducto.stream().allMatch(l -> l.getVolumenPendienteM3().signum() <= 0);

            pedido.setVolumenEntregadoM3(sumEntregado);
            pedido.setVolumenPendienteM3(sumPendiente);
            pedido.setEstatusGeneral(todasLasLineasCompletas ? "completo" : "parcial");
        }

        Pedido guardado = pedidoRepository.save(pedido);
        if ("completo".equals(guardado.getEstatusGeneral())) {
            notificarAsesor(guardado, bearerToken);
        }

        String descripcionEntrega = "Se registro una entrega de " + request.getMetrosEntregados() + " m3"
                + (request.getRemisionId() != null ? " (remision " + request.getRemisionId() + ")" : "");
        bitacoraService.registrar("pedido", guardado.getId(), "entrega", descripcionEntrega, null);

        return toResponse(guardado);
    }

    private void notificarAsesor(Pedido pedido, String bearerToken) {
        if (pedido.getAsesor() == null || pedido.getAsesor().getUsuarioId() == null) {
            return;
        }
        try {
            notificacionClient.crear(pedido.getAsesor().getUsuarioId(), "pedido", "Pedido entregado",
                    "El pedido " + pedido.getFolio() + " ya se entrego por completo.", "pedido", pedido.getId(), bearerToken);
        } catch (Exception e) {
            log.warn("No se pudo notificar al asesor del pedido {}: {}", pedido.getId(), e.getMessage());
        }
    }

    private void acumularEntregaEnEncabezado(Pedido pedido, BigDecimal metrosEntregados) {
        BigDecimal totalEntregado = pedido.getVolumenEntregadoM3().add(metrosEntregados);
        if (totalEntregado.compareTo(pedido.getVolumenSolicitadoM3()) > 0) {
            totalEntregado = pedido.getVolumenSolicitadoM3();
        }
        pedido.setVolumenEntregadoM3(totalEntregado);
        pedido.setVolumenPendienteM3(pedido.getVolumenSolicitadoM3().subtract(totalEntregado));
        pedido.setEstatusGeneral(totalEntregado.compareTo(pedido.getVolumenSolicitadoM3()) >= 0 ? "completo" : "parcial");
    }

    private PedidoDetalle resolverLinea(List<PedidoDetalle> lineas, RegistrarEntregaRequest request) {
        if (request.getPedidoDetalleId() != null) {
            return lineas.stream()
                    .filter(l -> l.getId().equals(request.getPedidoDetalleId()))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "La linea " + request.getPedidoDetalleId() + " no pertenece a este pedido"));
        }
        if (request.getProductoId() != null) {
            return lineas.stream()
                    .filter(l -> request.getProductoId().equals(l.getProductoId()))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "El pedido no tiene una linea para el producto " + request.getProductoId()));
        }
        if (lineas.size() == 1) {
            return lineas.get(0);
        }
        throw new IllegalArgumentException(
                "Este pedido tiene mas de un producto; debe indicar pedidoDetalleId o productoId para saber que linea entregar");
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
        List<PedidoDetalle> lineas = pedidoDetalleRepository.findByPedidoId(pedido.getId());
        BigDecimal montoTotal = lineas.stream()
                .map(PedidoDetalle::getPrecioTotal)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

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
                .volumenSolicitadoM3(pedido.getVolumenSolicitadoM3())
                .volumenEntregadoM3(pedido.getVolumenEntregadoM3())
                .volumenPendienteM3(pedido.getVolumenPendienteM3())
                .tipoServicio(pedido.getTipoServicio())
                .fechaProgramada(pedido.getFechaProgramada())
                .horarioEntrega(pedido.getHorarioEntrega())
                .elementoConstructivoId(pedido.getElementoConstructivoId())
                .distanciaKm(pedido.getDistanciaKm())
                .condicionPago(pedido.getCondicionPago())
                .diasCredito(pedido.getDiasCredito())
                .formaPago(pedido.getCotizacion() != null ? pedido.getCotizacion().getFormaPago() : null)
                .montoTotal(montoTotal)
                .estatusPagoAutorizacion(pedido.getEstatusPagoAutorizacion())
                .estatusLogisticaAutorizacion(pedido.getEstatusLogisticaAutorizacion())
                .estatusGeneral(pedido.getEstatusGeneral())
                .motivoRechazo(pedido.getMotivoRechazo())
                .createdAt(pedido.getCreatedAt())
                .creadoPorUsuario(pedido.getCreadoPorUsuario())
                .actualizadoPorUsuario(pedido.getActualizadoPorUsuario())
                .actualizadoEn(pedido.getActualizadoEn())
                .productos(lineas.stream().map(this::toItemResponse).toList())
                .build();
    }

    private PedidoItemResponse toItemResponse(PedidoDetalle linea) {
        return PedidoItemResponse.builder()
                .id(linea.getId())
                .tipoLinea(linea.getTipoLinea())
                .productoId(linea.getProductoId())
                .volumenSolicitadoM3(linea.getVolumenSolicitadoM3())
                .volumenEntregadoM3(linea.getVolumenEntregadoM3())
                .volumenPendienteM3(linea.getVolumenPendienteM3())
                .precioUnitario(linea.getPrecioUnitario())
                .precioTotal(linea.getPrecioTotal())
                .descripcion(linea.getDescripcion())
                .build();
    }

    /**
     * Recordatorio de vencimiento de credito: una vez al dia, revisa pedidos a credito (no
     * rechazados, con dias de credito y fecha programada capturados, no avisados todavia) y, si
     * faltan {@link #DIAS_AVISO_VENCIMIENTO_CREDITO} dias o menos para vencer (incluye ya vencidos,
     * diasRestantes negativo), notifica al asesor del pedido; si ya esta vencido, notifica ademas a
     * todos los usuarios del rol Direccion. Se dispara una sola vez por pedido (recordatorioCreditoEnviado).
     *
     * Vencimiento = fechaProgramada + diasCredito -- no se guarda, se calcula (mismo criterio que
     * el vencimiento de ordenes de compra a proveedores en finanzas-service). No verifica si el
     * pedido ya se liquido de verdad (eso vive en Pago, finanzas-service); si Pagos/Finanzas marca
     * el pedido como "liquidado" al cobrar, deja de ser candidato aqui.
     */
    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void enviarRecordatoriosCredito() {
        List<Pedido> candidatos = pedidoRepository
                .findByCondicionPagoAndDiasCreditoIsNotNullAndFechaProgramadaIsNotNullAndRecordatorioCreditoEnviadoFalseAndEstatusGeneralNot(
                        "credito", "rechazado");

        LocalDate hoy = LocalDate.now();
        for (Pedido pedido : candidatos) {
            LocalDate vencimiento = pedido.getFechaProgramada().plusDays(pedido.getDiasCredito());
            long diasRestantes = ChronoUnit.DAYS.between(hoy, vencimiento);
            if (diasRestantes > DIAS_AVISO_VENCIMIENTO_CREDITO) {
                continue;
            }

            boolean vencido = diasRestantes < 0;
            String titulo = vencido ? "Crédito vencido" : "Crédito por vencer";
            String mensaje = vencido
                    ? "El crédito del pedido " + pedido.getFolio() + " venció hace " + (-diasRestantes) + " día(s) (" + vencimiento + ")."
                    : "El crédito del pedido " + pedido.getFolio() + " vence en " + diasRestantes + " día(s) (" + vencimiento + ").";

            boolean enviado = true;

            if (pedido.getAsesor() != null && pedido.getAsesor().getUsuarioId() != null) {
                try {
                    notificacionClient.crear(pedido.getAsesor().getUsuarioId(), "credito", titulo, mensaje, "pedido", pedido.getId(), null);
                } catch (Exception e) {
                    log.warn("No se pudo notificar al asesor del pedido {} sobre vencimiento de credito: {}", pedido.getId(), e.getMessage());
                    enviado = false;
                }
            }

            if (vencido) {
                try {
                    List<AuthClient.UsuarioInfo> direccion = authClient.listarUsuariosPorRol(ROL_DIRECCION);
                    for (AuthClient.UsuarioInfo usuario : direccion) {
                        notificacionClient.crear(usuario.getId(), "credito", titulo, mensaje, "pedido", pedido.getId(), null);
                    }
                } catch (Exception e) {
                    log.warn("No se pudo notificar a Direccion sobre credito vencido del pedido {}: {}", pedido.getId(), e.getMessage());
                    enviado = false;
                }
            }

            if (enviado) {
                pedido.setRecordatorioCreditoEnviado(true);
                pedidoRepository.save(pedido);
            }
        }
    }
}
