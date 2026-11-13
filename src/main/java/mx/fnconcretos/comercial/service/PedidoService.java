package mx.fnconcretos.comercial.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.fnconcretos.comercial.client.AuthClient;
import mx.fnconcretos.comercial.client.FinanzasClient;
import mx.fnconcretos.comercial.client.NotificacionClient;
import mx.fnconcretos.comercial.client.OperacionesClient;
import mx.fnconcretos.comercial.client.WhatsAppClient;
import mx.fnconcretos.comercial.dto.request.AjustarVolumenPedidoRequest;
import mx.fnconcretos.comercial.dto.request.CambiarProductoPedidoRequest;
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
import mx.fnconcretos.comercial.entity.AsesorComercial;
import mx.fnconcretos.comercial.repository.AsesorComercialRepository;
import mx.fnconcretos.comercial.repository.PedidoAutorizacionRepository;
import mx.fnconcretos.comercial.repository.PedidoDetalleRepository;
import mx.fnconcretos.comercial.repository.PedidoRepository;
import mx.fnconcretos.comercial.security.JwtPrincipal;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private final FinanzasClient finanzasClient;
    private final BitacoraService bitacoraService;
    private final AsesorComercialRepository asesorRepository;

    @Transactional(readOnly = true)
    public List<PedidoResponse> listar(Long clienteId, String estatusGeneral, String q, Long plantaId, Long asesorId, JwtPrincipal principal) {
        return listar(clienteId, estatusGeneral, q, plantaId, asesorId, null, null, principal);
    }

    public List<PedidoResponse> listar(Long clienteId, String estatusGeneral, String q, Long plantaId, Long asesorId,
                                        LocalDate desde, LocalDate hasta, JwtPrincipal principal) {
        Specification<Pedido> spec = Specification.where(null);

        if (clienteId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("cliente").get("id"), clienteId));
        }
        if (plantaId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("plantaId"), plantaId));
        }
        // Un Asesor Comercial solo ve sus propios pedidos -- se ignora cualquier asesorId distinto
        // que venga en el query string (hallazgo real 2026-11-13, §comercial/finanzas: GET
        // /pedidos?clienteId= de un cliente ajeno regresaba 200 con los pedidos reales de ese
        // cliente sin filtrar por asesor en absoluto, no solo "200 []" como parecio en la prueba).
        if (esAsesorComercial(principal)) {
            Long miAsesorId = miAsesor(principal).getId();
            spec = spec.and((root, query, cb) -> cb.equal(root.get("asesor").get("id"), miAsesorId));
        } else if (asesorId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("asesor").get("id"), asesorId));
        }
        if (estatusGeneral != null && !estatusGeneral.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("estatusGeneral"), estatusGeneral));
        }
        if (q != null && !q.isBlank()) {
            String like = "%" + q.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("folio")), like));
        }
        if (desde != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("fechaProgramada"), desde));
        }
        if (hasta != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("fechaProgramada"), hasta));
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
            notificarFinanzasPedidoCompletado(guardado, bearerToken);
        }

        String descripcionEntrega = "Se registro una entrega de " + request.getMetrosEntregados() + " m3"
                + (request.getRemisionId() != null ? " (remision " + request.getRemisionId() + ")" : "");
        bitacoraService.registrar("pedido", guardado.getId(), "entrega", descripcionEntrega, null);

        return toResponse(guardado);
    }

    /** Ajusta el volumen (y por lo tanto el precioTotal) de UNA linea de producto del pedido --
     * junta de jefes 2026-11-05: "el cliente decide que en su ultima remision que aun va a salir
     * necesita mas o menos m3". Nunca baja de lo ya entregado (linea.volumenEntregadoM3). Decisiones
     * de negocio confirmadas por el usuario: (1) no se vuelve a pedir autorizacion de pago aunque el
     * monto suba -- se permite cobrar la diferencia despues o contra entrega; (2) este ajuste solo
     * ocurre el mismo dia de la entrega, antes de cualquier corte de comisiones, asi que nunca debe
     * tocar una comision ya generada (no se intenta detectar ni corregir eso aqui).
     */
    @Transactional
    public PedidoResponse ajustarVolumen(Long id, Long detalleId, AjustarVolumenPedidoRequest request, JwtPrincipal principal) {
        Pedido pedido = buscarOFallar(id);
        PedidoDetalle linea = pedidoDetalleRepository.findById(detalleId)
                .filter(l -> l.getPedido().getId().equals(id))
                .orElseThrow(() -> new ResourceNotFoundException("La linea " + detalleId + " no pertenece al pedido " + id));

        if (!"producto".equals(linea.getTipoLinea())) {
            throw new IllegalArgumentException("Solo se puede ajustar el volumen de una linea tipo 'producto'");
        }
        if (request.getNuevoVolumenM3().compareTo(linea.getVolumenEntregadoM3()) < 0) {
            throw new EstadoInvalidoException("El nuevo volumen (" + request.getNuevoVolumenM3()
                    + " m3) no puede ser menor a lo ya entregado de esta linea (" + linea.getVolumenEntregadoM3() + " m3)");
        }

        BigDecimal volumenAnterior = linea.getVolumenSolicitadoM3();
        BigDecimal precioTotalAnterior = linea.getPrecioTotal();

        linea.setVolumenSolicitadoM3(request.getNuevoVolumenM3());
        linea.setVolumenPendienteM3(request.getNuevoVolumenM3().subtract(linea.getVolumenEntregadoM3()));
        linea.setPrecioTotal(linea.getPrecioUnitario().multiply(request.getNuevoVolumenM3()).setScale(2, RoundingMode.HALF_UP));
        pedidoDetalleRepository.save(linea);

        List<PedidoDetalle> lineasProducto = pedidoDetalleRepository.findByPedidoId(id).stream()
                .filter(l -> "producto".equals(l.getTipoLinea()))
                .toList();
        BigDecimal sumSolicitado = lineasProducto.stream().map(PedidoDetalle::getVolumenSolicitadoM3).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal sumPendiente = lineasProducto.stream().map(PedidoDetalle::getVolumenPendienteM3).reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean todasLasLineasCompletas = lineasProducto.stream().allMatch(l -> l.getVolumenPendienteM3().signum() <= 0);

        pedido.setVolumenSolicitadoM3(sumSolicitado);
        pedido.setVolumenPendienteM3(sumPendiente);
        // Si el pedido ya estaba "completo" y el ajuste sube el volumen (vuelve a quedar pendiente),
        // regresa a "parcial" -- mismo criterio que registrarEntrega, solo cuando el estatus actual
        // ya es uno de entrega (nunca toca pendiente_autorizacion_pago/autorizado/rechazado).
        if (ESTATUS_PERMITEN_ENTREGA.contains(pedido.getEstatusGeneral()) || "completo".equals(pedido.getEstatusGeneral())) {
            pedido.setEstatusGeneral(todasLasLineasCompletas ? "completo" : "parcial");
        }

        Pedido guardado = pedidoRepository.save(pedido);

        String detalle = "Volumen ajustado de la linea " + detalleId + ": " + volumenAnterior + " m3 ($" + precioTotalAnterior
                + ") -> " + request.getNuevoVolumenM3() + " m3 ($" + linea.getPrecioTotal() + "). Motivo: " + request.getMotivo();
        bitacoraService.registrar("pedido", guardado.getId(), "ajuste_volumen", detalle, principal != null ? principal.user() : null);

        return toResponse(guardado);
    }

    /** Cambia el producto (ej. resistencia) de una linea del pedido -- junta de jefes 2026-11-05/06:
     * "el cliente requiera algun cambio de resistencia en su pedido". Cada resistencia es un
     * producto distinto en el catalogo (propia receta, propio precio) -- este cambio solo mueve
     * productoId/precio de la linea; la receta correcta la resuelve sola operaciones-service por
     * productoId+planta al generar la remision, sin cambios adicionales.
     *
     * Solo permitido mientras la linea no tenga nada entregado -- caso confirmado por el usuario:
     * "el pedido todavia no ha salido ninguna remision". NO protege contra una remision ya
     * "generada"/"programada" en operaciones-service que aun no entrega (ese caso quedo
     * explicitamente pendiente de definir, el usuario pidio dejarlo asi por ahora).
     */
    @Transactional
    public PedidoResponse cambiarProducto(Long id, Long detalleId, CambiarProductoPedidoRequest request, JwtPrincipal principal) {
        Pedido pedido = buscarOFallar(id);
        PedidoDetalle linea = pedidoDetalleRepository.findById(detalleId)
                .filter(l -> l.getPedido().getId().equals(id))
                .orElseThrow(() -> new ResourceNotFoundException("La linea " + detalleId + " no pertenece al pedido " + id));

        if (!"producto".equals(linea.getTipoLinea())) {
            throw new IllegalArgumentException("Solo se puede cambiar el producto de una linea tipo 'producto'");
        }
        if (linea.getVolumenEntregadoM3().signum() > 0) {
            throw new EstadoInvalidoException("No se puede cambiar el producto de la linea " + detalleId + ": ya tiene "
                    + linea.getVolumenEntregadoM3() + " m3 entregados. Solo se permite antes de que salga la primera remision.");
        }

        Long productoAnterior = linea.getProductoId();
        BigDecimal precioAnterior = linea.getPrecioUnitario();

        linea.setProductoId(request.getNuevoProductoId());
        linea.setPrecioUnitarioBase(request.getNuevoPrecioUnitario());
        linea.setPrecioUnitario(request.getNuevoPrecioUnitario());
        linea.setPrecioTotal(request.getNuevoPrecioUnitario().multiply(linea.getVolumenSolicitadoM3()).setScale(2, RoundingMode.HALF_UP));
        pedidoDetalleRepository.save(linea);

        String detalle = "Producto de la linea " + detalleId + " cambiado de #" + productoAnterior + " ($" + precioAnterior
                + "/m3) a #" + request.getNuevoProductoId() + " ($" + request.getNuevoPrecioUnitario() + "/m3). Motivo: " + request.getMotivo();
        bitacoraService.registrar("pedido", pedido.getId(), "cambio_producto", detalle, principal != null ? principal.user() : null);

        return toResponse(pedido);
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

    /** Genera la prefactura automatica en finanzas-service cuando el cliente tiene
     * modalidadFacturacion=por_pedido -- finanzas-service resuelve esa preferencia por su cuenta
     * (ver PrefacturaService.procesarEventoPedidoCompletado), aqui solo se avisa del evento. Una
     * falla nunca bloquea que el pedido quede marcado como completo. */
    private void notificarFinanzasPedidoCompletado(Pedido pedido, String bearerToken) {
        try {
            finanzasClient.notificarPedidoCompletado(pedido.getId(), bearerToken);
        } catch (Exception e) {
            log.warn("No se pudo notificar a finanzas-service el pedido completado {}: {}", pedido.getId(), e.getMessage());
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

    private boolean esAsesorComercial(JwtPrincipal principal) {
        return principal != null && "Asesor Comercial".equals(principal.rol());
    }

    private AsesorComercial miAsesor(JwtPrincipal principal) {
        return asesorRepository.findByUsuarioId(principal.usuarioId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El usuario " + principal.usuarioId() + " no tiene un asesor comercial asociado"));
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
                .clienteTelefono(pedido.getCliente().getTelefono())
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
                .zonaPrecio(pedido.getZonaPrecio())
                .porcentajeRecargoForaneo(pedido.getPorcentajeRecargoForaneo())
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
                .precioUnitarioBase(linea.getPrecioUnitarioBase())
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
