package mx.fnconcretos.comercial.service;

import lombok.RequiredArgsConstructor;
import mx.fnconcretos.comercial.dto.request.CotizacionItemRequest;
import mx.fnconcretos.comercial.dto.request.CotizacionRequest;
import mx.fnconcretos.comercial.dto.request.ConvertirPedidoRequest;
import mx.fnconcretos.comercial.dto.request.EstatusRequest;
import mx.fnconcretos.comercial.dto.response.CotizacionItemResponse;
import mx.fnconcretos.comercial.dto.response.CotizacionResponse;
import mx.fnconcretos.comercial.dto.response.PedidoItemResponse;
import mx.fnconcretos.comercial.dto.response.PedidoResponse;
import mx.fnconcretos.comercial.entity.AsesorComercial;
import mx.fnconcretos.comercial.entity.Cliente;
import mx.fnconcretos.comercial.entity.ContactoCliente;
import mx.fnconcretos.comercial.entity.Cotizacion;
import mx.fnconcretos.comercial.entity.CotizacionDetalle;
import mx.fnconcretos.comercial.entity.Obra;
import mx.fnconcretos.comercial.entity.Pedido;
import mx.fnconcretos.comercial.entity.PedidoDetalle;
import mx.fnconcretos.comercial.exception.EstadoInvalidoException;
import mx.fnconcretos.comercial.exception.ResourceNotFoundException;
import mx.fnconcretos.comercial.repository.ContactoClienteRepository;
import mx.fnconcretos.comercial.repository.CotizacionDetalleRepository;
import mx.fnconcretos.comercial.repository.CotizacionRepository;
import mx.fnconcretos.comercial.repository.PedidoDetalleRepository;
import mx.fnconcretos.comercial.repository.PedidoRepository;
import mx.fnconcretos.comercial.security.JwtPrincipal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CotizacionService {

    private final CotizacionRepository cotizacionRepository;
    private final CotizacionDetalleRepository cotizacionDetalleRepository;
    private final PedidoRepository pedidoRepository;
    private final PedidoDetalleRepository pedidoDetalleRepository;
    private final ClienteService clienteService;
    private final ObraService obraService;
    private final ContactoClienteRepository contactoClienteRepository;
    private final AsesorComercialService asesorService;

    @Value("${negocio.descuento.max-efectivo}")
    private BigDecimal descuentoMaxEfectivo;

    @Value("${negocio.descuento.max-factura}")
    private BigDecimal descuentoMaxFactura;

    @Transactional(readOnly = true)
    public List<CotizacionResponse> listar(Long clienteId, String estatus) {
        Specification<Cotizacion> spec = Specification.where(null);

        if (clienteId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("cliente").get("id"), clienteId));
        }
        if (estatus != null && !estatus.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("estatus"), estatus));
        }

        return cotizacionRepository.findAll(spec).stream().map(this::toResponse).toList();
    }

    @Transactional
    public CotizacionResponse crear(CotizacionRequest request, JwtPrincipal principal) {
        Cliente cliente = clienteService.buscarOFallar(request.getClienteId());
        Obra obra = request.getObraId() != null ? obraService.buscarOFallar(request.getObraId()) : null;
        ContactoCliente contacto = request.getContactoId() != null ? buscarContactoOFallar(request.getContactoId()) : null;
        AsesorComercial asesor = request.getAsesorId() != null ? asesorService.buscarOFallar(request.getAsesorId()) : null;

        BigDecimal descuento = request.getPorcentajeDescuento() != null ? request.getPorcentajeDescuento() : BigDecimal.ZERO;
        validarDescuento(descuento, request.getFormaPago(), principal);

        List<CotizacionItemRequest> items = normalizarItems(request);
        CotizacionItemRequest primero = items.get(0);
        BigDecimal volumenTotal = items.stream().map(CotizacionItemRequest::getVolumenM3).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal precioTotal = items.stream()
                .map(item -> precioLineaConDescuento(item, descuento))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Cotizacion cotizacion = Cotizacion.builder()
                .folio(generarFolio())
                .cliente(cliente)
                .obra(obra)
                .contacto(contacto)
                .plantaId(request.getPlantaId())
                .asesor(asesor)
                .productoId(primero.getProductoId())
                .volumenM3(volumenTotal)
                .tipoServicio(request.getTipoServicio() != null ? request.getTipoServicio() : "directo")
                .fechaSuministroEstimada(request.getFechaSuministroEstimada())
                .formaPago(request.getFormaPago() != null ? request.getFormaPago() : "efectivo")
                .requiereFactura(request.getRequiereFactura() != null ? request.getRequiereFactura() : false)
                .porcentajeDescuento(descuento)
                .precioUnitario(primero.getPrecioUnitario())
                .precioTotal(precioTotal)
                .build();

        Cotizacion guardada = cotizacionRepository.save(cotizacion);
        guardarDetalle(guardada, items, descuento);

        return toResponse(guardada);
    }

    @Transactional(readOnly = true)
    public CotizacionResponse obtener(Long id) {
        return toResponse(buscarOFallar(id));
    }

    @Transactional
    public CotizacionResponse actualizar(Long id, CotizacionRequest request, JwtPrincipal principal) {
        Cotizacion cotizacion = buscarOFallar(id);
        if ("convertida".equals(cotizacion.getEstatus())) {
            throw new EstadoInvalidoException("No se puede modificar una cotizacion ya convertida a pedido");
        }

        BigDecimal descuento = request.getPorcentajeDescuento() != null ? request.getPorcentajeDescuento() : BigDecimal.ZERO;
        validarDescuento(descuento, request.getFormaPago(), principal);

        List<CotizacionItemRequest> items = normalizarItems(request);
        CotizacionItemRequest primero = items.get(0);
        BigDecimal volumenTotal = items.stream().map(CotizacionItemRequest::getVolumenM3).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal precioTotal = items.stream()
                .map(item -> precioLineaConDescuento(item, descuento))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        cotizacion.setCliente(clienteService.buscarOFallar(request.getClienteId()));
        cotizacion.setObra(request.getObraId() != null ? obraService.buscarOFallar(request.getObraId()) : null);
        cotizacion.setContacto(request.getContactoId() != null ? buscarContactoOFallar(request.getContactoId()) : null);
        cotizacion.setPlantaId(request.getPlantaId());
        cotizacion.setAsesor(request.getAsesorId() != null ? asesorService.buscarOFallar(request.getAsesorId()) : null);
        cotizacion.setProductoId(primero.getProductoId());
        cotizacion.setVolumenM3(volumenTotal);
        if (request.getTipoServicio() != null) cotizacion.setTipoServicio(request.getTipoServicio());
        cotizacion.setFechaSuministroEstimada(request.getFechaSuministroEstimada());
        if (request.getFormaPago() != null) cotizacion.setFormaPago(request.getFormaPago());
        if (request.getRequiereFactura() != null) cotizacion.setRequiereFactura(request.getRequiereFactura());
        cotizacion.setPorcentajeDescuento(descuento);
        cotizacion.setPrecioUnitario(primero.getPrecioUnitario());
        cotizacion.setPrecioTotal(precioTotal);

        Cotizacion guardada = cotizacionRepository.save(cotizacion);
        cotizacionDetalleRepository.deleteByCotizacionId(guardada.getId());
        guardarDetalle(guardada, items, descuento);

        return toResponse(guardada);
    }

    @Transactional
    public CotizacionResponse cambiarEstatus(Long id, EstatusRequest request) {
        Cotizacion cotizacion = buscarOFallar(id);
        if ("convertida".equals(cotizacion.getEstatus())) {
            throw new EstadoInvalidoException("No se puede cambiar el estatus de una cotizacion ya convertida a pedido");
        }
        cotizacion.setEstatus(request.getEstatus());
        return toResponse(cotizacionRepository.save(cotizacion));
    }

    @Transactional
    public CotizacionResponse duplicar(Long id) {
        Cotizacion origen = buscarOFallar(id);

        Cotizacion copia = Cotizacion.builder()
                .folio(generarFolio())
                .cliente(origen.getCliente())
                .obra(origen.getObra())
                .contacto(origen.getContacto())
                .plantaId(origen.getPlantaId())
                .asesor(origen.getAsesor())
                .productoId(origen.getProductoId())
                .volumenM3(origen.getVolumenM3())
                .tipoServicio(origen.getTipoServicio())
                .fechaSuministroEstimada(origen.getFechaSuministroEstimada())
                .formaPago(origen.getFormaPago())
                .requiereFactura(origen.getRequiereFactura())
                .porcentajeDescuento(origen.getPorcentajeDescuento())
                .precioUnitario(origen.getPrecioUnitario())
                .precioTotal(origen.getPrecioTotal())
                .cotizacionOrigen(origen)
                .build();

        Cotizacion guardada = cotizacionRepository.save(copia);

        List<CotizacionDetalle> lineasOrigen = cotizacionDetalleRepository.findByCotizacionId(origen.getId());
        List<CotizacionDetalle> copiaLineas = lineasOrigen.stream()
                .map(linea -> CotizacionDetalle.builder()
                        .cotizacion(guardada)
                        .productoId(linea.getProductoId())
                        .volumenM3(linea.getVolumenM3())
                        .precioUnitario(linea.getPrecioUnitario())
                        .precioTotal(linea.getPrecioTotal())
                        .build())
                .toList();
        cotizacionDetalleRepository.saveAll(copiaLineas);

        return toResponse(guardada);
    }

    @Transactional
    public PedidoResponse convertirAPedido(Long id, ConvertirPedidoRequest request) {
        Cotizacion cotizacion = buscarOFallar(id);
        if (!"listo".equals(cotizacion.getEstatus())) {
            throw new EstadoInvalidoException("Solo se puede convertir a pedido una cotizacion en estatus 'listo'");
        }

        List<CotizacionDetalle> lineasCotizacion = cotizacionDetalleRepository.findByCotizacionId(cotizacion.getId());
        if (lineasCotizacion.isEmpty()) {
            // Cotizacion legado (creada antes del desglose por producto): una sola linea implicita.
            lineasCotizacion = List.of(CotizacionDetalle.builder()
                    .productoId(cotizacion.getProductoId())
                    .volumenM3(cotizacion.getVolumenM3())
                    .precioUnitario(cotizacion.getPrecioUnitario())
                    .precioTotal(cotizacion.getPrecioTotal())
                    .build());
        }

        Pedido pedido = Pedido.builder()
                .folio(generarFolioPedido())
                .cotizacion(cotizacion)
                .cliente(cotizacion.getCliente())
                .obra(cotizacion.getObra())
                .plantaId(cotizacion.getPlantaId())
                .asesor(cotizacion.getAsesor())
                .productoId(cotizacion.getProductoId())
                .volumenSolicitadoM3(cotizacion.getVolumenM3())
                .volumenPendienteM3(cotizacion.getVolumenM3())
                .tipoServicio(cotizacion.getTipoServicio())
                .fechaProgramada(request.getFechaProgramada())
                .condicionPago(request.getCondicionPago())
                .diasCredito(request.getDiasCredito())
                .build();

        Pedido guardado = pedidoRepository.save(pedido);

        List<PedidoDetalle> lineasPedido = lineasCotizacion.stream()
                .map(linea -> PedidoDetalle.builder()
                        .pedido(guardado)
                        .productoId(linea.getProductoId())
                        .volumenSolicitadoM3(linea.getVolumenM3())
                        .volumenPendienteM3(linea.getVolumenM3())
                        .precioUnitario(linea.getPrecioUnitario())
                        .precioTotal(linea.getPrecioTotal())
                        .build())
                .toList();
        pedidoDetalleRepository.saveAll(lineasPedido);

        cotizacion.setEstatus("convertida");
        cotizacionRepository.save(cotizacion);

        return toPedidoResponse(guardado, lineasPedido);
    }

    /** Si viene "productos" se usa tal cual; si no, productoId/volumenM3/precioUnitario del encabezado se tratan como linea unica. */
    private List<CotizacionItemRequest> normalizarItems(CotizacionRequest request) {
        if (request.getProductos() != null && !request.getProductos().isEmpty()) {
            return request.getProductos();
        }
        if (request.getProductoId() == null || request.getVolumenM3() == null || request.getPrecioUnitario() == null) {
            throw new IllegalArgumentException("Debe indicar 'productos' o bien productoId/volumenM3/precioUnitario");
        }
        return List.of(new CotizacionItemRequest(request.getProductoId(), request.getVolumenM3(), request.getPrecioUnitario()));
    }

    private BigDecimal precioLineaConDescuento(CotizacionItemRequest item, BigDecimal descuento) {
        BigDecimal precioConDescuento = item.getPrecioUnitario()
                .multiply(BigDecimal.ONE.subtract(descuento.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)));
        return precioConDescuento.multiply(item.getVolumenM3()).setScale(2, RoundingMode.HALF_UP);
    }

    private void guardarDetalle(Cotizacion cotizacion, List<CotizacionItemRequest> items, BigDecimal descuento) {
        List<CotizacionDetalle> lineas = items.stream()
                .map(item -> CotizacionDetalle.builder()
                        .cotizacion(cotizacion)
                        .productoId(item.getProductoId())
                        .volumenM3(item.getVolumenM3())
                        .precioUnitario(item.getPrecioUnitario())
                        .precioTotal(precioLineaConDescuento(item, descuento))
                        .build())
                .toList();
        cotizacionDetalleRepository.saveAll(lineas);
    }

    private void validarDescuento(BigDecimal descuento, String formaPago, JwtPrincipal principal) {
        if (descuento == null || descuento.signum() <= 0) return;

        BigDecimal limite = "factura".equals(formaPago) ? descuentoMaxFactura : descuentoMaxEfectivo;
        if (descuento.compareTo(limite) > 0 && (principal == null || !principal.tienePermiso("cotizaciones.aplicar_descuento_especial"))) {
            throw new AccessDeniedException("El descuento de " + descuento + "% excede el limite de " + limite
                    + "% para forma de pago '" + formaPago + "'; se requiere el permiso cotizaciones.aplicar_descuento_especial");
        }
    }

    private String generarFolio() {
        String base = "COT-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long secuencia = cotizacionRepository.count() + 1;
        String folio;
        do {
            folio = base + "-" + String.format("%04d", secuencia++);
        } while (cotizacionRepository.existsByFolio(folio));
        return folio;
    }

    private String generarFolioPedido() {
        String base = "PED-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long secuencia = pedidoRepository.count() + 1;
        String folio;
        do {
            folio = base + "-" + String.format("%04d", secuencia++);
        } while (pedidoRepository.existsByFolio(folio));
        return folio;
    }

    protected Cotizacion buscarOFallar(Long id) {
        return cotizacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cotizacion no encontrada: " + id));
    }

    private ContactoCliente buscarContactoOFallar(Long id) {
        return contactoClienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contacto no encontrado: " + id));
    }

    private CotizacionResponse toResponse(Cotizacion cotizacion) {
        return CotizacionResponse.builder()
                .id(cotizacion.getId())
                .folio(cotizacion.getFolio())
                .clienteId(cotizacion.getCliente().getId())
                .clienteNombre(cotizacion.getCliente().getNombre())
                .obraId(cotizacion.getObra() != null ? cotizacion.getObra().getId() : null)
                .obraNombre(cotizacion.getObra() != null ? cotizacion.getObra().getNombre() : null)
                .contactoId(cotizacion.getContacto() != null ? cotizacion.getContacto().getId() : null)
                .plantaId(cotizacion.getPlantaId())
                .asesorId(cotizacion.getAsesor() != null ? cotizacion.getAsesor().getId() : null)
                .asesorNombre(cotizacion.getAsesor() != null ? cotizacion.getAsesor().getNombre() : null)
                .productoId(cotizacion.getProductoId())
                .volumenM3(cotizacion.getVolumenM3())
                .tipoServicio(cotizacion.getTipoServicio())
                .fechaSuministroEstimada(cotizacion.getFechaSuministroEstimada())
                .formaPago(cotizacion.getFormaPago())
                .requiereFactura(cotizacion.getRequiereFactura())
                .porcentajeDescuento(cotizacion.getPorcentajeDescuento())
                .precioUnitario(cotizacion.getPrecioUnitario())
                .precioUnitarioConDescuento(cotizacion.getPrecioUnitario()
                        .multiply(BigDecimal.ONE.subtract(cotizacion.getPorcentajeDescuento().divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)))
                        .setScale(2, RoundingMode.HALF_UP))
                .montoTotal(cotizacion.getPrecioTotal())
                .estatus(cotizacion.getEstatus())
                .cotizacionOrigenId(cotizacion.getCotizacionOrigen() != null ? cotizacion.getCotizacionOrigen().getId() : null)
                .createdAt(cotizacion.getCreatedAt())
                .productos(cotizacionDetalleRepository.findByCotizacionId(cotizacion.getId()).stream()
                        .map(this::toItemResponse).toList())
                .build();
    }

    private CotizacionItemResponse toItemResponse(CotizacionDetalle linea) {
        return CotizacionItemResponse.builder()
                .id(linea.getId())
                .productoId(linea.getProductoId())
                .volumenM3(linea.getVolumenM3())
                .precioUnitario(linea.getPrecioUnitario())
                .precioTotal(linea.getPrecioTotal())
                .build();
    }

    private PedidoResponse toPedidoResponse(Pedido pedido, List<PedidoDetalle> lineas) {
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
                .productos(lineas.stream().map(linea -> PedidoItemResponse.builder()
                        .id(linea.getId())
                        .productoId(linea.getProductoId())
                        .volumenSolicitadoM3(linea.getVolumenSolicitadoM3())
                        .volumenEntregadoM3(linea.getVolumenEntregadoM3())
                        .volumenPendienteM3(linea.getVolumenPendienteM3())
                        .precioUnitario(linea.getPrecioUnitario())
                        .precioTotal(linea.getPrecioTotal())
                        .build()).toList())
                .build();
    }
}
