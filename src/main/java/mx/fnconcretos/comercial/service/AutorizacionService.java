package mx.fnconcretos.comercial.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.fnconcretos.comercial.client.CatalogoClient;
import mx.fnconcretos.comercial.client.NotificacionClient;
import mx.fnconcretos.comercial.client.OperacionesClient;
import mx.fnconcretos.comercial.client.WhatsAppClient;
import mx.fnconcretos.comercial.dto.request.AutorizarRequest;
import mx.fnconcretos.comercial.dto.response.AutorizacionResponse;
import mx.fnconcretos.comercial.entity.Cliente;
import mx.fnconcretos.comercial.entity.Pedido;
import mx.fnconcretos.comercial.entity.PedidoAutorizacion;
import mx.fnconcretos.comercial.entity.PedidoDetalle;
import mx.fnconcretos.comercial.exception.EstadoInvalidoException;
import mx.fnconcretos.comercial.repository.PedidoAutorizacionRepository;
import mx.fnconcretos.comercial.repository.PedidoDetalleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutorizacionService {

    private static final String APROBADO = "aprobado";
    private static final String RECHAZADO = "rechazado";
    private static final Locale LOCALE_MX = new Locale("es", "MX");
    private static final DateTimeFormatter FORMATO_FECHA_ENTREGA = DateTimeFormatter.ofPattern("d 'de' MMMM", LOCALE_MX);

    /** Texto amigable del estatusGeneral del pedido, para el WhatsApp de "pedido confirmado" -- en
     * el disparo actual (justo tras aprobar logistica) siempre va a salir "autorizado", nunca
     * "pendiente de pago" (eso ya se aprobo en el paso anterior); si el negocio quiere reflejar
     * aqui si el CLIENTE ya liquido ese tiro en particular (no solo la autorizacion interna), hay
     * que cambiar el criterio para que consulte pagos/anticipos en finanzas-service. */
    private static final Map<String, String> ESTATUS_LABELS = Map.of(
            "pendiente_autorizacion_pago", "pendiente de pago",
            "pendiente_autorizacion_logistica", "pendiente de logística",
            "autorizado", "autorizado",
            "programado", "programado",
            "parcial", "en proceso",
            "completo", "completado",
            "rechazado", "rechazado");

    private final PedidoAutorizacionRepository autorizacionRepository;
    private final PedidoDetalleRepository pedidoDetalleRepository;
    private final PedidoService pedidoService;
    private final NotificacionClient notificacionClient;
    private final WhatsAppClient whatsAppClient;
    private final OperacionesClient operacionesClient;
    private final CatalogoClient catalogoClient;
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

    /**
     * Envia el WhatsApp de "pedido confirmado" al cliente -- formato "TIRO CONFIRMADO" (cliente,
     * fecha y hora, concreto, descarga, estatus, ubicacion del tiro). Nunca bloquea la autorizacion.
     * La plantilla ya NO lleva boton: la ubicacion va como variable de texto en el cuerpo (WhatsApp
     * la hipervincula solo), asi que el ultimo parametro de enviarPlantilla es null.
     */
    private void notificarClienteConfirmacion(Pedido pedido, String bearerToken) {
        Cliente cliente = pedido.getCliente();
        if (cliente == null || cliente.getTelefono() == null || cliente.getTelefono().isBlank()) {
            return;
        }
        try {
            List<PedidoDetalle> lineas = pedidoDetalleRepository.findByPedidoId(pedido.getId());
            List<String> parametros = List.of(
                    cliente.getNombre(),
                    formatearFechaHoraTiro(pedido),
                    formatearConcreto(lineas, bearerToken),
                    "bomba".equals(pedido.getTipoServicio()) ? "Bombeado" : "Tiro directo",
                    ESTATUS_LABELS.getOrDefault(pedido.getEstatusGeneral(), pedido.getEstatusGeneral()),
                    pedido.getObra() != null && pedido.getObra().getGoogleMapsLink() != null
                            ? pedido.getObra().getGoogleMapsLink() : "por confirmar");
            whatsAppClient.enviarPlantilla(cliente.getTelefono(), "pedido_confirmado", "es_MX", parametros);
        } catch (Exception e) {
            log.warn("No se pudo enviar el WhatsApp de pedido confirmado al cliente del pedido {}: {}", pedido.getId(), e.getMessage());
        }
    }

    /** Ej. "Sábado 14 de noviembre, 10:00 a. m.". "por confirmar" si falta la fecha programada. */
    private String formatearFechaHoraTiro(Pedido pedido) {
        if (pedido.getFechaProgramada() == null) {
            return "por confirmar";
        }
        String diaSemana = pedido.getFechaProgramada().getDayOfWeek().getDisplayName(TextStyle.FULL, LOCALE_MX);
        diaSemana = diaSemana.substring(0, 1).toUpperCase(LOCALE_MX) + diaSemana.substring(1);
        String fecha = diaSemana + " " + pedido.getFechaProgramada().format(FORMATO_FECHA_ENTREGA);
        if (pedido.getHorarioEntrega() == null) {
            return fecha;
        }
        LocalTime hora = pedido.getHorarioEntrega();
        int hora12 = hora.getHour() % 12 == 0 ? 12 : hora.getHour() % 12;
        String ampm = hora.getHour() < 12 ? "a. m." : "p. m.";
        return fecha + ", " + String.format("%d:%02d %s", hora12, hora.getMinute(), ampm);
    }

    /** Ej. "4 m³ de f'c 100 kg/cm²" -- resistencia del primer producto de la lista (si el pedido
     * mezcla resistencias distintas en un mismo tiro, solo se reporta la del primero; caso raro). */
    private String formatearConcreto(List<PedidoDetalle> lineas, String bearerToken) {
        List<PedidoDetalle> productos = lineas.stream().filter(l -> "producto".equals(l.getTipoLinea())).toList();
        BigDecimal volumen = productos.stream()
                .map(PedidoDetalle::getVolumenSolicitadoM3)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        String volumenTexto = volumen.stripTrailingZeros().toPlainString() + " m³";

        String resistencia = productos.stream()
                .map(PedidoDetalle::getProductoId)
                .filter(Objects::nonNull)
                .findFirst()
                .map(productoId -> {
                    try {
                        return catalogoClient.obtenerProducto(productoId, bearerToken).getResistencia();
                    } catch (Exception e) {
                        return null;
                    }
                })
                .orElse(null);

        return resistencia != null && !resistencia.isBlank()
                ? volumenTexto + " de f'c " + resistencia + " kg/cm²"
                : volumenTexto;
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
