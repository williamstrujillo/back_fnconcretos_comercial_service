package mx.fnconcretos.comercial.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.fnconcretos.comercial.client.CatalogoClient;
import mx.fnconcretos.comercial.dto.response.CotizacionPublicaResponse;
import mx.fnconcretos.comercial.dto.response.TokenCompartidoResponse;
import mx.fnconcretos.comercial.entity.Cotizacion;
import mx.fnconcretos.comercial.entity.CotizacionCompartidaToken;
import mx.fnconcretos.comercial.entity.CotizacionDetalle;
import mx.fnconcretos.comercial.entity.Obra;
import mx.fnconcretos.comercial.exception.ResourceNotFoundException;
import mx.fnconcretos.comercial.repository.CotizacionCompartidaTokenRepository;
import mx.fnconcretos.comercial.repository.CotizacionDetalleRepository;
import mx.fnconcretos.comercial.repository.CotizacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/** Link publico (sin login) para que el cliente final vea/descargue su cotizacion, en el mismo
 * formato imprimible ya existente -- se comparte por WhatsApp junto con la plantilla
 * "cotizacion_lista". Mismo patron que el rastreo publico de pedidos (operaciones-service):
 * token corto reutilizable, pero aqui el snapshot se congela en JSON al compartir (una cotizacion
 * es un documento de negocio, no debe cambiar si se edita despues). */
@Slf4j
@Service
@RequiredArgsConstructor
public class CotizacionCompartidaService {

    private static final String ALFABETO_TOKEN = "abcdefghijkmnpqrstuvwxyz23456789"; // sin 0/o/1/l/i
    private static final int LONGITUD_TOKEN = 10;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter FORMATO_FECHA_ENTREGA = DateTimeFormatter.ofPattern("dd / MM / yyyy");

    private final CotizacionCompartidaTokenRepository tokenRepository;
    private final CotizacionRepository cotizacionRepository;
    private final CotizacionDetalleRepository cotizacionDetalleRepository;
    private final CatalogoClient catalogoClient;
    private final ObjectMapper objectMapper;

    @Transactional
    public TokenCompartidoResponse obtenerOCrearToken(Long cotizacionId, String bearerToken) {
        CotizacionCompartidaToken existente = tokenRepository.findByCotizacionId(cotizacionId).orElse(null);
        if (existente != null) {
            return toTokenResponse(existente);
        }

        Cotizacion cotizacion = cotizacionRepository.findById(cotizacionId)
                .orElseThrow(() -> new ResourceNotFoundException("Cotizacion no encontrada: " + cotizacionId));

        String snapshotJson = construirSnapshotJson(cotizacion, bearerToken);

        CotizacionCompartidaToken nuevo = CotizacionCompartidaToken.builder()
                .cotizacionId(cotizacionId)
                .token(generarToken())
                .snapshotJson(snapshotJson)
                .build();
        return toTokenResponse(tokenRepository.save(nuevo));
    }

    @Transactional(readOnly = true)
    public CotizacionPublicaResponse consultarPorToken(String token) {
        CotizacionCompartidaToken entidad = tokenRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Link de cotizacion no valido o vencido"));
        try {
            return objectMapper.readValue(entidad.getSnapshotJson(), CotizacionPublicaResponse.class);
        } catch (Exception e) {
            log.error("No se pudo deserializar el snapshot de la cotizacion compartida {}: {}", entidad.getCotizacionId(), e.getMessage());
            throw new IllegalStateException("No se pudo leer la cotizacion compartida");
        }
    }

    private String construirSnapshotJson(Cotizacion cotizacion, String bearerToken) {
        List<CotizacionDetalle> lineas = cotizacionDetalleRepository.findByCotizacionId(cotizacion.getId());

        String plantaNombre = null;
        CotizacionPublicaResponse.EmpresaInfo empresa = null;
        if (cotizacion.getPlantaId() != null) {
            try {
                CatalogoClient.PlantaTarifas planta = catalogoClient.obtenerTarifas(cotizacion.getPlantaId(), bearerToken);
                plantaNombre = planta != null ? planta.getNombre() : null;
                if (planta != null && planta.getEmpresaId() != null) {
                    CatalogoClient.EmpresaInfo empresaInfo = catalogoClient.obtenerEmpresa(planta.getEmpresaId(), bearerToken);
                    if (empresaInfo != null) {
                        empresa = CotizacionPublicaResponse.EmpresaInfo.builder()
                                .nombre(empresaInfo.getNombre())
                                .razonSocial(empresaInfo.getRazonSocial())
                                .rfc(empresaInfo.getRfc())
                                .telefono(empresaInfo.getTelefono())
                                .email(empresaInfo.getEmail())
                                .direccion(empresaInfo.getDireccion())
                                .build();
                    }
                }
            } catch (Exception e) {
                log.warn("No se pudo resolver planta/empresa {} para el snapshot de la cotizacion {}: {}",
                        cotizacion.getPlantaId(), cotizacion.getId(), e.getMessage());
            }
        }

        CotizacionPublicaResponse snapshot = CotizacionPublicaResponse.builder()
                .tipoDocumento("Cotización")
                .folio(cotizacion.getFolio())
                .fecha(cotizacion.getCreatedAt())
                .planta(plantaNombre)
                .empresa(empresa)
                .cliente(construirCliente(cotizacion))
                .entrega(construirEntrega(cotizacion))
                .lineas(construirLineas(lineas, bearerToken))
                .totalVolumen(cotizacion.getVolumenM3())
                .totales(construirTotales(cotizacion, lineas))
                .vendedor(construirVendedor(cotizacion))
                .observaciones(cotizacion.getObservaciones())
                .build();

        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el snapshot de la cotizacion " + cotizacion.getId(), e);
        }
    }

    private CotizacionPublicaResponse.ClienteInfo construirCliente(Cotizacion cotizacion) {
        if (cotizacion.getCliente() == null) return null;
        return CotizacionPublicaResponse.ClienteInfo.builder()
                .nombre(cotizacion.getCliente().getNombre())
                .telefono(cotizacion.getCliente().getTelefono())
                .rfc(cotizacion.getCliente().getRfc())
                .build();
    }

    private CotizacionPublicaResponse.EntregaInfo construirEntrega(Cotizacion cotizacion) {
        Obra obra = cotizacion.getObra();
        String direccion = null;
        if (obra != null) {
            direccion = (obra.getDireccion() != null && !obra.getDireccion().isBlank())
                    ? List.of(obra.getDireccion(), obra.getColonia(), obra.getCiudad()).stream()
                            .filter(s -> s != null && !s.isBlank())
                            .collect(Collectors.joining(", "))
                    : obra.getNombre();
        }

        String fechaHoraServicio = cotizacion.getFechaSuministroEstimada() != null
                ? cotizacion.getFechaSuministroEstimada().format(FORMATO_FECHA_ENTREGA)
                : null;

        return CotizacionPublicaResponse.EntregaInfo.builder()
                .direccion(direccion)
                .elemento(null)
                .notas(null)
                .fechaHoraServicio(fechaHoraServicio)
                .condicionPago("factura".equals(cotizacion.getFormaPago()) ? "Factura" : "Efectivo")
                .bomba("bomba".equals(cotizacion.getTipoServicio()) ? "Sí" : null)
                .autorizo(null)
                .build();
    }

    private List<CotizacionPublicaResponse.LineaInfo> construirLineas(List<CotizacionDetalle> lineas, String bearerToken) {
        return lineas.stream().map(linea -> {
            String descripcion;
            if ("producto".equals(linea.getTipoLinea())) {
                descripcion = resolverNombreProducto(linea.getProductoId(), bearerToken);
            } else {
                descripcion = linea.getDescripcion() != null ? linea.getDescripcion() : etiquetaTipoLinea(linea.getTipoLinea());
            }
            BigDecimal importe = linea.getPrecioTotal();
            return CotizacionPublicaResponse.LineaInfo.builder()
                    .codigo(null)
                    .descripcion(descripcion)
                    .cantidad(linea.getVolumenM3())
                    .unidad("M3")
                    .precioUnitario(linea.getPrecioUnitario())
                    .importe(importe)
                    .build();
        }).toList();
    }

    private String resolverNombreProducto(Long productoId, String bearerToken) {
        if (productoId == null) return null;
        try {
            CatalogoClient.ProductoInfo producto = catalogoClient.obtenerProducto(productoId, bearerToken);
            return producto != null ? producto.getNombre() : "#" + productoId;
        } catch (Exception e) {
            log.warn("No se pudo resolver el producto {} para el snapshot de cotizacion: {}", productoId, e.getMessage());
            return "#" + productoId;
        }
    }

    private String etiquetaTipoLinea(String tipoLinea) {
        return switch (tipoLinea) {
            case "bombeo" -> "Bombeo";
            case "flete_vacio" -> "Flete por vacío";
            default -> "Otro";
        };
    }

    private CotizacionPublicaResponse.TotalesInfo construirTotales(Cotizacion cotizacion, List<CotizacionDetalle> lineas) {
        BigDecimal subtotal = lineas.stream()
                .map(l -> (l.getVolumenM3() != null ? l.getVolumenM3() : BigDecimal.ZERO)
                        .multiply(l.getPrecioUnitario() != null ? l.getPrecioUnitario() : BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        return CotizacionPublicaResponse.TotalesInfo.builder()
                .subtotal(subtotal)
                .total(cotizacion.getPrecioTotal())
                .build();
    }

    private CotizacionPublicaResponse.VendedorInfo construirVendedor(Cotizacion cotizacion) {
        if (cotizacion.getAsesor() == null) return null;
        return CotizacionPublicaResponse.VendedorInfo.builder()
                .nombre(cotizacion.getAsesor().getNombre())
                .telefono(null)
                .build();
    }

    private String generarToken() {
        String token;
        do {
            StringBuilder sb = new StringBuilder(LONGITUD_TOKEN);
            for (int i = 0; i < LONGITUD_TOKEN; i++) {
                sb.append(ALFABETO_TOKEN.charAt(RANDOM.nextInt(ALFABETO_TOKEN.length())));
            }
            token = sb.toString();
        } while (tokenRepository.existsByToken(token));
        return token;
    }

    private TokenCompartidoResponse toTokenResponse(CotizacionCompartidaToken entity) {
        return TokenCompartidoResponse.builder()
                .cotizacionId(entity.getCotizacionId())
                .token(entity.getToken())
                .build();
    }
}
