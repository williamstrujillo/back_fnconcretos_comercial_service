package mx.fnconcretos.comercial.service;

import lombok.RequiredArgsConstructor;
import mx.fnconcretos.comercial.client.CatalogoClient;
import mx.fnconcretos.comercial.client.GoogleMapsClient;
import mx.fnconcretos.comercial.client.WhatsAppClient;
import mx.fnconcretos.comercial.dto.request.CotizacionItemRequest;
import mx.fnconcretos.comercial.dto.request.CotizacionRequest;
import mx.fnconcretos.comercial.dto.request.ConvertirPedidoRequest;
import mx.fnconcretos.comercial.dto.request.EstatusRequest;
import mx.fnconcretos.comercial.dto.response.CotizacionItemResponse;
import mx.fnconcretos.comercial.dto.response.CotizacionResponse;
import mx.fnconcretos.comercial.dto.response.PedidoItemResponse;
import mx.fnconcretos.comercial.dto.response.PedidoResponse;
import mx.fnconcretos.comercial.dto.response.WhatsAppEnvioResponse;
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
import java.time.LocalDateTime;
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
    private final CatalogoClient catalogoClient;
    private final GoogleMapsClient googleMapsClient;
    private final WhatsAppClient whatsAppClient;
    private final CotizacionCompartidaService cotizacionCompartidaService;
    private final BitacoraService bitacoraService;

    @Value("${negocio.descuento.min-sin-factura}")
    private BigDecimal descuentoMinSinFactura;

    @Value("${negocio.descuento.max-sin-factura}")
    private BigDecimal descuentoMaxSinFactura;

    @Value("${negocio.descuento.min-con-factura}")
    private BigDecimal descuentoMinConFactura;

    @Value("${negocio.descuento.max-con-factura}")
    private BigDecimal descuentoMaxConFactura;

    @Transactional(readOnly = true)
    public List<CotizacionResponse> listar(Long clienteId, String estatus, String q) {
        Specification<Cotizacion> spec = Specification.where(null);

        if (clienteId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("cliente").get("id"), clienteId));
        }
        if (estatus != null && !estatus.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("estatus"), estatus));
        }
        if (q != null && !q.isBlank()) {
            String like = "%" + q.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("folio")), like));
        }

        return cotizacionRepository.findAll(spec).stream().map(this::toResponse).toList();
    }

    @Transactional
    public CotizacionResponse crear(CotizacionRequest request, JwtPrincipal principal, String bearerToken) {
        Cliente cliente = clienteService.buscarOFallar(request.getClienteId());
        Obra obra = request.getObraId() != null ? obraService.buscarOFallar(request.getObraId()) : null;
        ContactoCliente contacto = request.getContactoId() != null ? buscarContactoOFallar(request.getContactoId()) : null;
        AsesorComercial asesor = request.getAsesorId() != null ? asesorService.buscarOFallar(request.getAsesorId()) : null;

        BigDecimal descuento = request.getPorcentajeDescuento() != null ? request.getPorcentajeDescuento() : BigDecimal.ZERO;
        validarDescuento(descuento, request.getRequiereFactura(), principal);
        validarDescuentosLinea(request.getProductos(), principal);

        CatalogoClient.PlantaTarifas tarifas = catalogoClient.obtenerTarifas(request.getPlantaId(), bearerToken);
        List<CotizacionDetalle> lineas = calcularLineas(request.getProductos(), tarifas, descuento, principal);
        BigDecimal volumenTotal = volumenTotalProducto(lineas);
        BigDecimal subtotal = precioTotalLineas(lineas);
        BigDecimal precioUnitarioPrimero = precioUnitarioPrimerProducto(lineas);
        boolean requiereFactura = Boolean.TRUE.equals(request.getRequiereFactura());
        BigDecimal porcentajeIvaAplicado = requiereFactura ? porcentajeIvaDePlanta(tarifas) : null;
        BigDecimal iva = calcularIva(subtotal, porcentajeIvaAplicado);
        BigDecimal distanciaKm = calcularDistanciaKm(obra, tarifas);

        Cotizacion cotizacion = Cotizacion.builder()
                .folio(generarFolio())
                .cliente(cliente)
                .obra(obra)
                .contacto(contacto)
                .plantaId(request.getPlantaId())
                .asesor(asesor)
                .volumenM3(volumenTotal)
                .tipoServicio(request.getTipoServicio() != null ? request.getTipoServicio() : "directo")
                .fechaSuministroEstimada(request.getFechaSuministroEstimada())
                .horarioEntrega(request.getHorarioEntrega())
                .elementoConstructivoId(request.getElementoConstructivoId())
                .distanciaKm(distanciaKm)
                .formaPago(request.getFormaPago() != null ? request.getFormaPago() : "efectivo")
                .requiereFactura(requiereFactura)
                .porcentajeDescuento(descuento)
                .precioUnitario(precioUnitarioPrimero)
                .subtotal(subtotal)
                .iva(iva)
                .porcentajeIva(porcentajeIvaAplicado)
                .precioTotal(subtotal.add(iva))
                .observaciones(request.getObservaciones())
                .creadoPorUsuario(principal != null ? principal.user() : null)
                .actualizadoPorUsuario(principal != null ? principal.user() : null)
                .actualizadoEn(LocalDateTime.now())
                .build();

        Cotizacion guardada = cotizacionRepository.save(cotizacion);
        lineas.forEach(linea -> linea.setCotizacion(guardada));
        cotizacionDetalleRepository.saveAll(lineas);

        bitacoraService.registrar("cotizacion", guardada.getId(), "creacion",
                "Creo la cotizacion " + guardada.getFolio(), guardada.getCreadoPorUsuario());

        return toResponse(guardada);
    }

    @Transactional(readOnly = true)
    public CotizacionResponse obtener(Long id) {
        return toResponse(buscarOFallar(id));
    }

    /**
     * Notifica al cliente por WhatsApp que su cotizacion ya esta lista, usando la plantilla
     * aprobada por Meta "cotizacion_lista" (es_MX). Disparo manual (boton "Enviar por WhatsApp" en
     * la ficha de cotizacion) — no automatico, para que el asesor decida el momento. Incluye el
     * boton con el link publico de la cotizacion (mismo patron que "pedido_confirmado").
     */
    @Transactional
    public WhatsAppEnvioResponse enviarWhatsApp(Long id, String bearerToken) {
        CotizacionResponse cotizacion = obtener(id);
        Cliente cliente = clienteService.buscarOFallar(cotizacion.getClienteId());
        if (cliente.getTelefono() == null || cliente.getTelefono().isBlank()) {
            throw new EstadoInvalidoException("El cliente no tiene telefono registrado");
        }

        String tokenCompartido = cotizacionCompartidaService.obtenerOCrearToken(id, bearerToken).getToken();

        List<String> parametros = List.of(
                cotizacion.getClienteNombre() != null ? cotizacion.getClienteNombre() : cliente.getNombre(),
                cotizacion.getFolio(),
                formatearMonto(cotizacion.getMontoTotal()),
                cotizacion.getAsesorNombre() != null ? cotizacion.getAsesorNombre() : "—"
        );
        whatsAppClient.enviarPlantilla(cliente.getTelefono(), "cotizacion_lista", "es_MX", parametros, tokenCompartido);

        return WhatsAppEnvioResponse.builder().enviado(true).telefono(cliente.getTelefono()).build();
    }

    private String formatearMonto(BigDecimal monto) {
        return monto == null ? "0.00" : String.format("%,.2f", monto);
    }

    @Transactional
    public CotizacionResponse actualizar(Long id, CotizacionRequest request, JwtPrincipal principal, String bearerToken) {
        Cotizacion cotizacion = buscarOFallar(id);
        if ("convertida".equals(cotizacion.getEstatus())) {
            throw new EstadoInvalidoException("No se puede modificar una cotizacion ya convertida a pedido");
        }

        BigDecimal descuento = request.getPorcentajeDescuento() != null ? request.getPorcentajeDescuento() : BigDecimal.ZERO;
        validarDescuento(descuento, request.getRequiereFactura(), principal);
        validarDescuentosLinea(request.getProductos(), principal);

        CatalogoClient.PlantaTarifas tarifas = catalogoClient.obtenerTarifas(request.getPlantaId(), bearerToken);
        List<CotizacionDetalle> lineas = calcularLineas(request.getProductos(), tarifas, descuento, principal);
        BigDecimal volumenTotal = volumenTotalProducto(lineas);
        BigDecimal subtotal = precioTotalLineas(lineas);
        BigDecimal precioUnitarioPrimero = precioUnitarioPrimerProducto(lineas);
        boolean requiereFactura = request.getRequiereFactura() != null ? request.getRequiereFactura() : Boolean.TRUE.equals(cotizacion.getRequiereFactura());
        BigDecimal porcentajeIvaAplicado = requiereFactura ? porcentajeIvaDePlanta(tarifas) : null;
        BigDecimal iva = calcularIva(subtotal, porcentajeIvaAplicado);

        // Solo se vuelve a llamar a Google Maps si cambio la obra/planta o si nunca se pudo calcular
        // antes -- evita gastar cuota del API en cada edicion (ej. solo tocar el descuento).
        boolean obraOplantaCambio = !java.util.Objects.equals(cotizacion.getObra() != null ? cotizacion.getObra().getId() : null, request.getObraId())
                || !java.util.Objects.equals(cotizacion.getPlantaId(), request.getPlantaId());
        Obra obraNueva = request.getObraId() != null ? obraService.buscarOFallar(request.getObraId()) : null;
        BigDecimal distanciaKm = (obraOplantaCambio || cotizacion.getDistanciaKm() == null)
                ? calcularDistanciaKm(obraNueva, tarifas)
                : cotizacion.getDistanciaKm();

        cotizacion.setCliente(clienteService.buscarOFallar(request.getClienteId()));
        cotizacion.setObra(obraNueva);
        cotizacion.setContacto(request.getContactoId() != null ? buscarContactoOFallar(request.getContactoId()) : null);
        cotizacion.setPlantaId(request.getPlantaId());
        cotizacion.setAsesor(request.getAsesorId() != null ? asesorService.buscarOFallar(request.getAsesorId()) : null);
        cotizacion.setVolumenM3(volumenTotal);
        if (request.getTipoServicio() != null) cotizacion.setTipoServicio(request.getTipoServicio());
        cotizacion.setFechaSuministroEstimada(request.getFechaSuministroEstimada());
        cotizacion.setHorarioEntrega(request.getHorarioEntrega());
        cotizacion.setElementoConstructivoId(request.getElementoConstructivoId());
        cotizacion.setDistanciaKm(distanciaKm);
        if (request.getFormaPago() != null) cotizacion.setFormaPago(request.getFormaPago());
        cotizacion.setRequiereFactura(requiereFactura);
        cotizacion.setPorcentajeDescuento(descuento);
        cotizacion.setPrecioUnitario(precioUnitarioPrimero);
        cotizacion.setSubtotal(subtotal);
        cotizacion.setIva(iva);
        cotizacion.setPorcentajeIva(porcentajeIvaAplicado);
        cotizacion.setPrecioTotal(subtotal.add(iva));
        cotizacion.setObservaciones(request.getObservaciones());
        cotizacion.setActualizadoPorUsuario(principal != null ? principal.user() : null);
        cotizacion.setActualizadoEn(LocalDateTime.now());

        Cotizacion guardada = cotizacionRepository.save(cotizacion);
        cotizacionDetalleRepository.deleteByCotizacionId(guardada.getId());
        lineas.forEach(linea -> linea.setCotizacion(guardada));
        cotizacionDetalleRepository.saveAll(lineas);

        bitacoraService.registrar("cotizacion", guardada.getId(), "actualizacion",
                "Actualizo los datos de la cotizacion", guardada.getActualizadoPorUsuario());

        return toResponse(guardada);
    }

    @Transactional
    public CotizacionResponse cambiarEstatus(Long id, EstatusRequest request, JwtPrincipal principal) {
        Cotizacion cotizacion = buscarOFallar(id);
        if ("convertida".equals(cotizacion.getEstatus())) {
            throw new EstadoInvalidoException("No se puede cambiar el estatus de una cotizacion ya convertida a pedido");
        }
        String estatusAnterior = cotizacion.getEstatus();
        String usuario = principal != null ? principal.user() : null;
        cotizacion.setEstatus(request.getEstatus());
        cotizacion.setActualizadoPorUsuario(usuario);
        cotizacion.setActualizadoEn(LocalDateTime.now());
        Cotizacion guardada = cotizacionRepository.save(cotizacion);

        bitacoraService.registrar("cotizacion", guardada.getId(), "cambio_estatus",
                "Cambio el estatus de " + estatusAnterior + " a " + request.getEstatus(), usuario);

        return toResponse(guardada);
    }

    @Transactional
    public CotizacionResponse duplicar(Long id, JwtPrincipal principal) {
        Cotizacion origen = buscarOFallar(id);

        String usuario = principal != null ? principal.user() : null;
        Cotizacion copia = Cotizacion.builder()
                .folio(generarFolio())
                .cliente(origen.getCliente())
                .obra(origen.getObra())
                .contacto(origen.getContacto())
                .plantaId(origen.getPlantaId())
                .asesor(origen.getAsesor())
                .volumenM3(origen.getVolumenM3())
                .tipoServicio(origen.getTipoServicio())
                .fechaSuministroEstimada(origen.getFechaSuministroEstimada())
                .horarioEntrega(origen.getHorarioEntrega())
                .elementoConstructivoId(origen.getElementoConstructivoId())
                .distanciaKm(origen.getDistanciaKm())
                .formaPago(origen.getFormaPago())
                .requiereFactura(origen.getRequiereFactura())
                .porcentajeDescuento(origen.getPorcentajeDescuento())
                .precioUnitario(origen.getPrecioUnitario())
                .precioTotal(origen.getPrecioTotal())
                .observaciones(origen.getObservaciones())
                .cotizacionOrigen(origen)
                .creadoPorUsuario(usuario)
                .actualizadoPorUsuario(usuario)
                .actualizadoEn(LocalDateTime.now())
                .build();

        Cotizacion guardada = cotizacionRepository.save(copia);

        List<CotizacionDetalle> lineasOrigen = cotizacionDetalleRepository.findByCotizacionId(origen.getId());
        List<CotizacionDetalle> copiaLineas = lineasOrigen.stream()
                .map(linea -> CotizacionDetalle.builder()
                        .cotizacion(guardada)
                        .tipoLinea(linea.getTipoLinea())
                        .productoId(linea.getProductoId())
                        .volumenM3(linea.getVolumenM3())
                        .precioUnitario(linea.getPrecioUnitario())
                        .precioTotal(linea.getPrecioTotal())
                        .descripcion(linea.getDescripcion())
                        .build())
                .toList();
        cotizacionDetalleRepository.saveAll(copiaLineas);

        bitacoraService.registrar("cotizacion", guardada.getId(), "creacion",
                "Duplico la cotizacion " + origen.getFolio(), usuario);

        return toResponse(guardada);
    }

    @Transactional
    public PedidoResponse convertirAPedido(Long id, ConvertirPedidoRequest request, JwtPrincipal principal) {
        Cotizacion cotizacion = buscarOFallar(id);
        if (!"listo".equals(cotizacion.getEstatus())) {
            throw new EstadoInvalidoException("Solo se puede convertir a pedido una cotizacion en estatus 'listo'");
        }

        List<CotizacionDetalle> lineasCotizacion = cotizacionDetalleRepository.findByCotizacionId(cotizacion.getId());
        if (lineasCotizacion.isEmpty()) {
            throw new EstadoInvalidoException("La cotizacion " + id + " no tiene productos registrados, no se puede convertir a pedido");
        }

        String usuario = principal != null ? principal.user() : null;
        Pedido pedido = Pedido.builder()
                .folio(generarFolioPedido())
                .cotizacion(cotizacion)
                .cliente(cotizacion.getCliente())
                .obra(cotizacion.getObra())
                .plantaId(cotizacion.getPlantaId())
                .asesor(cotizacion.getAsesor())
                .volumenSolicitadoM3(cotizacion.getVolumenM3())
                .volumenPendienteM3(cotizacion.getVolumenM3())
                .tipoServicio(cotizacion.getTipoServicio())
                .fechaProgramada(request.getFechaProgramada())
                .horarioEntrega(cotizacion.getHorarioEntrega())
                .elementoConstructivoId(cotizacion.getElementoConstructivoId())
                .distanciaKm(cotizacion.getDistanciaKm())
                .condicionPago(request.getCondicionPago())
                .diasCredito(request.getDiasCredito())
                .creadoPorUsuario(usuario)
                .actualizadoPorUsuario(usuario)
                .actualizadoEn(LocalDateTime.now())
                .build();

        Pedido guardado = pedidoRepository.save(pedido);

        List<PedidoDetalle> lineasPedido = new java.util.ArrayList<>(lineasCotizacion.stream()
                .map(linea -> PedidoDetalle.builder()
                        .pedido(guardado)
                        .tipoLinea(linea.getTipoLinea())
                        .productoId(linea.getProductoId())
                        .volumenSolicitadoM3(linea.getVolumenM3())
                        .volumenPendienteM3(linea.getVolumenM3())
                        .precioUnitario(linea.getPrecioUnitario())
                        .precioTotal(linea.getPrecioTotal())
                        .descripcion(linea.getDescripcion())
                        .build())
                .toList());

        // El pedido no tiene un campo de header para el total (ni subtotal/iva) -- su monto se
        // deriva SIEMPRE sumando pedido_detalle.precioTotal (ver PedidoInfo.getImporteTotal en
        // finanzas-service, y el frontend). Para que ese total siga incluyendo el IVA despues de
        // convertir, se agrega una linea "otro" con el monto de iva ya calculado en la cotizacion
        // -- mas simple que agregar subtotal/iva/total a Pedido y actualizar todo lo que ya deriva
        // el total sumando lineas.
        if (cotizacion.getIva() != null && cotizacion.getIva().signum() > 0) {
            lineasPedido.add(PedidoDetalle.builder()
                    .pedido(guardado)
                    .tipoLinea("otro")
                    .volumenSolicitadoM3(BigDecimal.ZERO)
                    .volumenPendienteM3(BigDecimal.ZERO)
                    .precioUnitario(cotizacion.getIva())
                    .precioTotal(cotizacion.getIva())
                    .descripcion("IVA (" + cotizacion.getPorcentajeIva() + "%)")
                    .build());
        }
        pedidoDetalleRepository.saveAll(lineasPedido);

        cotizacion.setEstatus("convertida");
        cotizacionRepository.save(cotizacion);

        bitacoraService.registrar("cotizacion", cotizacion.getId(), "conversion",
                "Convirtio la cotizacion en el pedido " + guardado.getFolio(), usuario);
        bitacoraService.registrar("pedido", guardado.getId(), "creacion",
                "Creado a partir de la cotizacion " + cotizacion.getFolio(), usuario);

        return toPedidoResponse(guardado, lineasPedido);
    }

    /** Rol que no puede modificar el costo (precioUnitario) del flete por vacio -- puede editar el
     * volumen, pero el precio siempre se fuerza a la tarifa de la planta (se ignora en silencio lo
     * que mande, mismo criterio que Cliente.origenCaptacion="asignado" fuerza 1% de comision). */
    private static final String ROL_ASESOR_COMERCIAL = "Asesor Comercial";

    /**
     * Expande "productos" a las lineas reales a guardar: cada linea 'producto' genera
     * ademas, si aplica, su linea automatica de 'flete_vacio' (capacidadReferenciaM3/
     * precioPorM3Vacio de la planta); las lineas 'bombeo' sin volumen especificado
     * toman el volumen total de las lineas 'producto'. El descuento solo aplica a
     * lineas 'producto' (bombeo/flete_vacio son cargos logisticos, no negociables).
     */
    private List<CotizacionDetalle> calcularLineas(List<CotizacionItemRequest> items, CatalogoClient.PlantaTarifas tarifas, BigDecimal descuento, JwtPrincipal principal) {
        BigDecimal volumenTotalProducto = items.stream()
                .filter(this::esProducto)
                .map(item -> {
                    if (item.getVolumenM3() == null) {
                        throw new IllegalArgumentException("volumenM3 es obligatorio para lineas tipo 'producto'");
                    }
                    return item.getVolumenM3();
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // El asesor puede editar o borrar la linea de flete_vacio que el sistema sugiere (ver rama
        // "flete_vacio" abajo) -- si el request ya trae una explicita, se respeta tal cual y no se
        // genera ninguna automatica encima (evitaria duplicarla).
        boolean fleteVacioManual = items.stream().anyMatch(item -> "flete_vacio".equals(tipoLinea(item)));

        List<CotizacionDetalle> lineas = new java.util.ArrayList<>();
        for (CotizacionItemRequest item : items) {
            String tipo = tipoLinea(item);
            if ("producto".equals(tipo)) {
                if (item.getProductoId() == null) {
                    throw new IllegalArgumentException("productoId es obligatorio para lineas tipo 'producto'");
                }
                BigDecimal volumen = item.getVolumenM3();
                BigDecimal descuentoLinea = item.getPorcentajeDescuentoLinea() != null ? item.getPorcentajeDescuentoLinea() : BigDecimal.ZERO;
                lineas.add(CotizacionDetalle.builder()
                        .tipoLinea("producto")
                        .productoId(item.getProductoId())
                        .volumenM3(volumen)
                        .precioUnitario(item.getPrecioUnitario())
                        .precioTotal(precioConDescuento(item.getPrecioUnitario(), volumen, descuento, descuentoLinea))
                        .porcentajeDescuentoLinea(descuentoLinea)
                        .descripcion(item.getDescripcion())
                        .build());

                if (!fleteVacioManual) {
                    BigDecimal capacidad = tarifas.getCapacidadReferenciaM3();
                    // Solo se cobra vacio si el pedido completo NO alcanza la capacidad de
                    // referencia (ej. 5m3 de 7m3 -> vacio de 2m3). Un pedido de 20m3 no genera
                    // vacio aunque el ultimo viaje parcial no llene la olla: el cargo es por
                    // pedido chico, no por como se reparta en viajes.
                    if (capacidad != null && capacidad.signum() > 0 && volumen.compareTo(capacidad) < 0) {
                        BigDecimal vacio = capacidad.subtract(volumen);
                        BigDecimal precioVacio = tarifas.getPrecioPorM3Vacio() != null ? tarifas.getPrecioPorM3Vacio() : BigDecimal.ZERO;
                        lineas.add(CotizacionDetalle.builder()
                                .tipoLinea("flete_vacio")
                                .volumenM3(vacio)
                                .precioUnitario(precioVacio)
                                .precioTotal(vacio.multiply(precioVacio).setScale(2, RoundingMode.HALF_UP))
                                .descripcion("Flete por vacio")
                                .build());
                    }
                }
            } else if ("bombeo".equals(tipo)) {
                BigDecimal volumen = item.getVolumenM3() != null ? item.getVolumenM3() : volumenTotalProducto;
                lineas.add(CotizacionDetalle.builder()
                        .tipoLinea("bombeo")
                        .volumenM3(volumen)
                        .precioUnitario(item.getPrecioUnitario())
                        .precioTotal(volumen.multiply(item.getPrecioUnitario()).setScale(2, RoundingMode.HALF_UP))
                        .descripcion(item.getDescripcion() != null ? item.getDescripcion() : "Bombeo")
                        .build());
            } else if ("flete_vacio".equals(tipo)) {
                // El asesor edito (o creo a mano) esta linea -- se respeta el volumen/precio que
                // mande, completando con el default de la planta solo lo que falte (mismo
                // criterio que bombeo). Excepcion: Asesor Comercial no puede tocar el costo, el
                // precio siempre es la tarifa de la planta sin importar lo que haya mandado.
                BigDecimal precioVacioDefault = tarifas.getPrecioPorM3Vacio() != null ? tarifas.getPrecioPorM3Vacio() : BigDecimal.ZERO;
                BigDecimal volumen = item.getVolumenM3() != null ? item.getVolumenM3() : BigDecimal.ZERO;
                boolean esAsesorComercial = principal != null && ROL_ASESOR_COMERCIAL.equals(principal.rol());
                BigDecimal precioUnitario = !esAsesorComercial && item.getPrecioUnitario() != null
                        ? item.getPrecioUnitario()
                        : precioVacioDefault;
                lineas.add(CotizacionDetalle.builder()
                        .tipoLinea("flete_vacio")
                        .volumenM3(volumen)
                        .precioUnitario(precioUnitario)
                        .precioTotal(volumen.multiply(precioUnitario).setScale(2, RoundingMode.HALF_UP))
                        .descripcion(item.getDescripcion() != null ? item.getDescripcion() : "Flete por vacio")
                        .build());
            } else {
                if (item.getVolumenM3() == null) {
                    throw new IllegalArgumentException("volumenM3 es obligatorio para lineas tipo '" + tipo + "'");
                }
                lineas.add(CotizacionDetalle.builder()
                        .tipoLinea(tipo)
                        .volumenM3(item.getVolumenM3())
                        .precioUnitario(item.getPrecioUnitario())
                        .precioTotal(item.getVolumenM3().multiply(item.getPrecioUnitario()).setScale(2, RoundingMode.HALF_UP))
                        .descripcion(item.getDescripcion())
                        .build());
            }
        }
        return lineas;
    }

    private String tipoLinea(CotizacionItemRequest item) {
        return item.getTipoLinea() != null && !item.getTipoLinea().isBlank() ? item.getTipoLinea() : "producto";
    }

    private boolean esProducto(CotizacionItemRequest item) {
        return "producto".equals(tipoLinea(item));
    }

    /** Descuento general (cabecera) y descuento de linea se combinan multiplicativamente (igual que
     * "10% de descuento y luego 5% extra sobre lo ya rebajado" en retail) -- no aditivo, para que
     * nunca se pueda pasar de 100% combinando ambos por accidente. */
    private BigDecimal precioConDescuento(BigDecimal precioUnitario, BigDecimal volumen, BigDecimal descuento, BigDecimal descuentoLinea) {
        BigDecimal factorGeneral = BigDecimal.ONE.subtract(descuento.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
        BigDecimal factorLinea = BigDecimal.ONE.subtract(descuentoLinea.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
        return precioUnitario.multiply(factorGeneral).multiply(factorLinea).multiply(volumen).setScale(2, RoundingMode.HALF_UP);
    }

    /** "Cuantos m3 en total esta solicitando el cliente" = solo lineas de producto (bombeo/flete_vacio son cargos, no concreto). */
    private BigDecimal volumenTotalProducto(List<CotizacionDetalle> lineas) {
        return lineas.stream()
                .filter(l -> "producto".equals(l.getTipoLinea()))
                .map(CotizacionDetalle::getVolumenM3)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal precioTotalLineas(List<CotizacionDetalle> lineas) {
        return lineas.stream().map(CotizacionDetalle::getPrecioTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal precioUnitarioPrimerProducto(List<CotizacionDetalle> lineas) {
        return lineas.stream()
                .filter(l -> "producto".equals(l.getTipoLinea()))
                .findFirst()
                .map(CotizacionDetalle::getPrecioUnitario)
                .orElse(BigDecimal.ZERO);
    }

    /** Rango habilitado (no min a max) segun requiereFactura, sin importar formaPago: sin factura
     * descuentoMinSinFactura-descuentoMaxSinFactura (default 0-6%), con factura
     * descuentoMinConFactura-descuentoMaxConFactura (default 8-11%). Salirse del rango (por
     * arriba O por abajo) requiere el permiso cotizaciones.aplicar_descuento_especial. */
    private void validarDescuento(BigDecimal descuento, Boolean requiereFactura, JwtPrincipal principal) {
        BigDecimal valor = descuento != null ? descuento : BigDecimal.ZERO;
        boolean factura = Boolean.TRUE.equals(requiereFactura);
        BigDecimal min = factura ? descuentoMinConFactura : descuentoMinSinFactura;
        BigDecimal max = factura ? descuentoMaxConFactura : descuentoMaxSinFactura;

        boolean fueraDeRango = valor.compareTo(min) < 0 || valor.compareTo(max) > 0;
        if (fueraDeRango && (principal == null || !principal.tienePermiso("cotizaciones.aplicar_descuento_especial"))) {
            throw new AccessDeniedException("El descuento de " + valor + "% esta fuera del rango permitido (" + min + "% - " + max
                    + "%) para cotizaciones " + (factura ? "con" : "sin") + " factura; se requiere el permiso cotizaciones.aplicar_descuento_especial");
        }
    }

    /** Cualquier descuento de linea (aparte del general ya validado en validarDescuento) requiere el
     * mismo permiso especial -- si no, seria una forma trivial de saltarse el rango de descuento
     * general ya negociado (0-6%/8-11%) agregando "descuento extra" por producto sin control. */
    private void validarDescuentosLinea(List<CotizacionItemRequest> items, JwtPrincipal principal) {
        boolean tieneDescuentoLinea = items != null && items.stream()
                .anyMatch(item -> item.getPorcentajeDescuentoLinea() != null && item.getPorcentajeDescuentoLinea().signum() > 0);
        if (tieneDescuentoLinea && (principal == null || !principal.tienePermiso("cotizaciones.aplicar_descuento_especial"))) {
            throw new AccessDeniedException("Aplicar un descuento extra por producto requiere el permiso cotizaciones.aplicar_descuento_especial");
        }
    }

    /** Distancia real por carretera obra->planta, solo si ambas ubicaciones se conocen (ver
     * GoogleMapsClient) -- nunca lanza, si falla algo regresa null y la cotizacion se guarda igual. */
    private BigDecimal calcularDistanciaKm(Obra obra, CatalogoClient.PlantaTarifas tarifas) {
        if (obra == null || tarifas == null) return null;
        return googleMapsClient.calcularDistanciaKm(obra.getLatitud(), obra.getLongitud(), tarifas.getLatitud(), tarifas.getLongitud());
    }

    /** Tasa de IVA configurada en la planta (Planta.porcentajeIva, catalogo-service) -- puede
     * variar por planta, por eso no es una constante del sistema. 0 si la planta no trae el dato
     * (nunca deberia pasar, tiene default 16 desde el modelo, pero por si acaso). */
    private BigDecimal porcentajeIvaDePlanta(CatalogoClient.PlantaTarifas tarifas) {
        return tarifas != null && tarifas.getPorcentajeIva() != null ? tarifas.getPorcentajeIva() : BigDecimal.ZERO;
    }

    private BigDecimal calcularIva(BigDecimal subtotal, BigDecimal porcentajeIva) {
        if (porcentajeIva == null) return BigDecimal.ZERO;
        return subtotal.multiply(porcentajeIva).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
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
                .volumenM3(cotizacion.getVolumenM3())
                .tipoServicio(cotizacion.getTipoServicio())
                .fechaSuministroEstimada(cotizacion.getFechaSuministroEstimada())
                .horarioEntrega(cotizacion.getHorarioEntrega())
                .elementoConstructivoId(cotizacion.getElementoConstructivoId())
                .distanciaKm(cotizacion.getDistanciaKm())
                .formaPago(cotizacion.getFormaPago())
                .requiereFactura(cotizacion.getRequiereFactura())
                .porcentajeDescuento(cotizacion.getPorcentajeDescuento())
                .precioUnitario(cotizacion.getPrecioUnitario())
                .precioUnitarioConDescuento(cotizacion.getPrecioUnitario()
                        .multiply(BigDecimal.ONE.subtract(cotizacion.getPorcentajeDescuento().divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)))
                        .setScale(2, RoundingMode.HALF_UP))
                .subtotal(cotizacion.getSubtotal())
                .iva(cotizacion.getIva())
                .porcentajeIva(cotizacion.getPorcentajeIva())
                .montoTotal(cotizacion.getPrecioTotal())
                .estatus(cotizacion.getEstatus())
                .cotizacionOrigenId(cotizacion.getCotizacionOrigen() != null ? cotizacion.getCotizacionOrigen().getId() : null)
                .observaciones(cotizacion.getObservaciones())
                .createdAt(cotizacion.getCreatedAt())
                .creadoPorUsuario(cotizacion.getCreadoPorUsuario())
                .actualizadoPorUsuario(cotizacion.getActualizadoPorUsuario())
                .actualizadoEn(cotizacion.getActualizadoEn())
                .productos(cotizacionDetalleRepository.findByCotizacionId(cotizacion.getId()).stream()
                        .map(this::toItemResponse).toList())
                .build();
    }

    private CotizacionItemResponse toItemResponse(CotizacionDetalle linea) {
        return CotizacionItemResponse.builder()
                .id(linea.getId())
                .tipoLinea(linea.getTipoLinea())
                .productoId(linea.getProductoId())
                .volumenM3(linea.getVolumenM3())
                .precioUnitario(linea.getPrecioUnitario())
                .precioTotal(linea.getPrecioTotal())
                .porcentajeDescuentoLinea(linea.getPorcentajeDescuentoLinea())
                .descripcion(linea.getDescripcion())
                .build();
    }

    private PedidoResponse toPedidoResponse(Pedido pedido, List<PedidoDetalle> lineas) {
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
                .formaPago(pedido.getCotizacion() != null ? pedido.getCotizacion().getFormaPago() : null)
                .montoTotal(montoTotal)
                .condicionPago(pedido.getCondicionPago())
                .diasCredito(pedido.getDiasCredito())
                .estatusPagoAutorizacion(pedido.getEstatusPagoAutorizacion())
                .estatusLogisticaAutorizacion(pedido.getEstatusLogisticaAutorizacion())
                .estatusGeneral(pedido.getEstatusGeneral())
                .motivoRechazo(pedido.getMotivoRechazo())
                .createdAt(pedido.getCreatedAt())
                .creadoPorUsuario(pedido.getCreadoPorUsuario())
                .actualizadoPorUsuario(pedido.getActualizadoPorUsuario())
                .actualizadoEn(pedido.getActualizadoEn())
                .productos(lineas.stream().map(linea -> PedidoItemResponse.builder()
                        .id(linea.getId())
                        .tipoLinea(linea.getTipoLinea())
                        .productoId(linea.getProductoId())
                        .volumenSolicitadoM3(linea.getVolumenSolicitadoM3())
                        .volumenEntregadoM3(linea.getVolumenEntregadoM3())
                        .volumenPendienteM3(linea.getVolumenPendienteM3())
                        .precioUnitario(linea.getPrecioUnitario())
                        .precioTotal(linea.getPrecioTotal())
                        .descripcion(linea.getDescripcion())
                        .build()).toList())
                .build();
    }
}
