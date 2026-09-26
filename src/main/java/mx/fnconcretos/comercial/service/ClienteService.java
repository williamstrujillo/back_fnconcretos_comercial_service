package mx.fnconcretos.comercial.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.fnconcretos.comercial.client.FinanzasClient;
import mx.fnconcretos.comercial.dto.request.ClienteRequest;
import mx.fnconcretos.comercial.dto.request.EstatusRequest;
import mx.fnconcretos.comercial.dto.response.ClienteResponse;
import mx.fnconcretos.comercial.dto.response.EstadoCuentaResponse;
import mx.fnconcretos.comercial.entity.AsesorComercial;
import mx.fnconcretos.comercial.entity.Cliente;
import mx.fnconcretos.comercial.exception.ConflictException;
import mx.fnconcretos.comercial.exception.ResourceNotFoundException;
import mx.fnconcretos.comercial.repository.AsesorComercialRepository;
import mx.fnconcretos.comercial.repository.ClienteRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final AsesorComercialRepository asesorRepository;
    private final FinanzasClient finanzasClient;

    @Transactional(readOnly = true)
    public List<ClienteResponse> listar(String tipo, String estatus, String q) {
        Specification<Cliente> spec = Specification.where(null);

        if (tipo != null && !tipo.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("tipo"), tipo));
        }
        if (estatus != null && !estatus.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("estatus"), estatus));
        }
        if (q != null && !q.isBlank()) {
            String like = "%" + q.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("nombre")), like),
                    cb.like(cb.lower(root.get("numeroCliente")), like)));
        }

        return clienteRepository.findAll(spec).stream().map(this::toResponse).toList();
    }

    @Transactional
    public ClienteResponse crear(ClienteRequest request) {
        String numeroCliente = request.getNumeroCliente() != null && !request.getNumeroCliente().isBlank()
                ? request.getNumeroCliente()
                : generarNumeroCliente();
        if (clienteRepository.existsByNumeroCliente(numeroCliente)) {
            throw new ConflictException("Ya existe un cliente con numero " + numeroCliente);
        }

        String origenCaptacion = request.getOrigenCaptacion() != null ? request.getOrigenCaptacion() : "asignado";

        Cliente cliente = Cliente.builder()
                .numeroCliente(numeroCliente)
                .nombre(request.getNombre())
                .tipo(request.getTipo() != null ? request.getTipo() : "particular")
                .rfc(request.getRfc())
                .telefono(request.getTelefono())
                .whatsappDisponible(request.getWhatsappDisponible() != null ? request.getWhatsappDisponible() : false)
                .correo(request.getCorreo())
                .requiereFacturaDefault(request.getRequiereFacturaDefault() != null ? request.getRequiereFacturaDefault() : false)
                .asesorAsignado(resolverAsesor(request.getAsesorAsignadoId()))
                .limiteCredito(request.getLimiteCredito() != null ? request.getLimiteCredito() : BigDecimal.ZERO)
                .diasCredito(request.getDiasCredito() != null ? request.getDiasCredito() : 0)
                .origenCaptacion(origenCaptacion)
                .porcentajeComision(resolverPorcentajeComision(origenCaptacion, request.getPorcentajeComision()))
                .build();

        return toResponse(clienteRepository.save(cliente));
    }

    /** Siguiente folio "CLI-0001", "CLI-0002"... a partir del mayor numero existente con ese formato. */
    private String generarNumeroCliente() {
        int max = clienteRepository.findAll().stream()
                .map(Cliente::getNumeroCliente)
                .filter(n -> n != null && n.matches("CLI-\\d+"))
                .mapToInt(n -> Integer.parseInt(n.substring(4)))
                .max()
                .orElse(0);
        return String.format("CLI-%04d", max + 1);
    }

    @Transactional(readOnly = true)
    public ClienteResponse obtener(Long id) {
        return toResponse(buscarOFallar(id));
    }

    @Transactional
    public ClienteResponse actualizar(Long id, ClienteRequest request) {
        Cliente cliente = buscarOFallar(id);

        if (!cliente.getNumeroCliente().equals(request.getNumeroCliente())
                && clienteRepository.existsByNumeroCliente(request.getNumeroCliente())) {
            throw new ConflictException("Ya existe un cliente con numero " + request.getNumeroCliente());
        }

        cliente.setNumeroCliente(request.getNumeroCliente());
        cliente.setNombre(request.getNombre());
        if (request.getTipo() != null) cliente.setTipo(request.getTipo());
        cliente.setRfc(request.getRfc());
        cliente.setTelefono(request.getTelefono());
        if (request.getWhatsappDisponible() != null) cliente.setWhatsappDisponible(request.getWhatsappDisponible());
        cliente.setCorreo(request.getCorreo());
        if (request.getRequiereFacturaDefault() != null) cliente.setRequiereFacturaDefault(request.getRequiereFacturaDefault());
        cliente.setAsesorAsignado(resolverAsesor(request.getAsesorAsignadoId()));
        if (request.getLimiteCredito() != null) cliente.setLimiteCredito(request.getLimiteCredito());
        if (request.getDiasCredito() != null) cliente.setDiasCredito(request.getDiasCredito());
        if (request.getOrigenCaptacion() != null) cliente.setOrigenCaptacion(request.getOrigenCaptacion());
        cliente.setPorcentajeComision(resolverPorcentajeComision(cliente.getOrigenCaptacion(), request.getPorcentajeComision()));

        return toResponse(clienteRepository.save(cliente));
    }

    /**
     * asignado: comision fija en 1.00%, no negociable. prospectado: comision negociada,
     * pero nunca menor a 1.00%.
     */
    private BigDecimal resolverPorcentajeComision(String origenCaptacion, BigDecimal porcentajeSolicitado) {
        if ("asignado".equals(origenCaptacion)) {
            return new BigDecimal("1.00");
        }
        if ("prospectado".equals(origenCaptacion)) {
            BigDecimal porcentaje = porcentajeSolicitado != null ? porcentajeSolicitado : new BigDecimal("1.00");
            if (porcentaje.compareTo(new BigDecimal("1.00")) < 0) {
                throw new IllegalArgumentException("El porcentaje de comision para un cliente prospectado debe ser de al menos 1.00%");
            }
            return porcentaje;
        }
        throw new IllegalArgumentException("origenCaptacion debe ser 'asignado' o 'prospectado'");
    }

    @Transactional
    public ClienteResponse cambiarEstatus(Long id, EstatusRequest request) {
        Cliente cliente = buscarOFallar(id);
        cliente.setEstatus(request.getEstatus());
        return toResponse(clienteRepository.save(cliente));
    }

    /**
     * OJO: a proposito SIN @Transactional -- este metodo hace una llamada HTTP saliente bloqueante
     * hacia finanzas-service (que a su vez llama de vuelta a este servicio). Si se envuelve en una
     * transaccion, Spring retiene una conexion de HikariCP reservada durante TODA esa llamada de
     * red; con varias peticiones concurrentes (ej. la pantalla de estado de cuenta de clientes, que
     * antes pedia una por cliente en paralelo) el pool se agota y las peticiones truenan con 500
     * antes de llegar siquiera al try/catch de abajo. buscarOFallar() ya corre en su propia
     * transaccion implicita de Spring Data, no necesita una envolvente.
     */
    public EstadoCuentaResponse estadoCuenta(Long id, String bearerToken) {
        buscarOFallar(id);
        try {
            FinanzasClient.EstadoCuentaClienteInfo info = finanzasClient.obtenerEstadoCuenta(id, bearerToken);
            return EstadoCuentaResponse.builder()
                    .clienteId(id)
                    .disponible(info.isDisponible())
                    .saldoActual(info.getSaldoActual())
                    .adeudoVencido(info.getAdeudoVencido())
                    .anticiposDisponibles(info.getAnticiposDisponibles())
                    .moroso(info.isMoroso())
                    .build();
        } catch (Exception e) {
            log.warn("No se pudo obtener el estado de cuenta del cliente {} desde finanzas-service: {}", id, e.getMessage());
            return EstadoCuentaResponse.builder()
                    .clienteId(id)
                    .disponible(false)
                    .mensaje("No se pudo consultar el estado de cuenta en este momento")
                    .build();
        }
    }

    /**
     * Estado de cuenta de varios clientes en UNA sola llamada a finanzas-service (en vez de que el
     * frontend dispare N peticiones en paralelo, una por cliente -- eso agotaba el pool de conexiones
     * de ambos servicios bajo carga, ver la correccion de @Transactional arriba en este mismo commit).
     */
    public List<EstadoCuentaResponse> estadoCuentaBatch(List<Long> ids, String bearerToken) {
        try {
            List<FinanzasClient.EstadoCuentaClienteInfo> infos = finanzasClient.obtenerEstadoCuentaBatch(ids, bearerToken);
            Map<Long, FinanzasClient.EstadoCuentaClienteInfo> porId = infos.stream()
                    .collect(Collectors.toMap(FinanzasClient.EstadoCuentaClienteInfo::getClienteId, i -> i));
            return ids.stream()
                    .map(id -> {
                        FinanzasClient.EstadoCuentaClienteInfo info = porId.get(id);
                        if (info == null) {
                            return EstadoCuentaResponse.builder()
                                    .clienteId(id)
                                    .disponible(false)
                                    .mensaje("No se pudo consultar el estado de cuenta en este momento")
                                    .build();
                        }
                        return EstadoCuentaResponse.builder()
                                .clienteId(id)
                                .disponible(info.isDisponible())
                                .saldoActual(info.getSaldoActual())
                                .adeudoVencido(info.getAdeudoVencido())
                                .anticiposDisponibles(info.getAnticiposDisponibles())
                                .moroso(info.isMoroso())
                                .build();
                    })
                    .toList();
        } catch (Exception e) {
            log.warn("No se pudo obtener el estado de cuenta batch de los clientes {} desde finanzas-service: {}", ids, e.getMessage());
            return ids.stream()
                    .map(id -> EstadoCuentaResponse.builder()
                            .clienteId(id)
                            .disponible(false)
                            .mensaje("No se pudo consultar el estado de cuenta en este momento")
                            .build())
                    .toList();
        }
    }

    protected Cliente buscarOFallar(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado: " + id));
    }

    private AsesorComercial resolverAsesor(Long asesorId) {
        if (asesorId == null) return null;
        return asesorRepository.findById(asesorId)
                .orElseThrow(() -> new ResourceNotFoundException("Asesor no encontrado: " + asesorId));
    }

    ClienteResponse toResponse(Cliente cliente) {
        return ClienteResponse.builder()
                .id(cliente.getId())
                .numeroCliente(cliente.getNumeroCliente())
                .nombre(cliente.getNombre())
                .tipo(cliente.getTipo())
                .rfc(cliente.getRfc())
                .telefono(cliente.getTelefono())
                .whatsappDisponible(cliente.getWhatsappDisponible())
                .correo(cliente.getCorreo())
                .requiereFacturaDefault(cliente.getRequiereFacturaDefault())
                .asesorAsignadoId(cliente.getAsesorAsignado() != null ? cliente.getAsesorAsignado().getId() : null)
                .asesorAsignadoNombre(cliente.getAsesorAsignado() != null ? cliente.getAsesorAsignado().getNombre() : null)
                .limiteCredito(cliente.getLimiteCredito())
                .diasCredito(cliente.getDiasCredito())
                .origenCaptacion(cliente.getOrigenCaptacion())
                .porcentajeComision(cliente.getPorcentajeComision())
                .estatus(cliente.getEstatus())
                .createdAt(cliente.getCreatedAt())
                .build();
    }
}
