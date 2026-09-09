package mx.fnconcretos.comercial.service;

import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final AsesorComercialRepository asesorRepository;

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
        if (clienteRepository.existsByNumeroCliente(request.getNumeroCliente())) {
            throw new ConflictException("Ya existe un cliente con numero " + request.getNumeroCliente());
        }

        String origenCaptacion = request.getOrigenCaptacion() != null ? request.getOrigenCaptacion() : "asignado";

        Cliente cliente = Cliente.builder()
                .numeroCliente(request.getNumeroCliente())
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

    @Transactional(readOnly = true)
    public EstadoCuentaResponse estadoCuenta(Long id) {
        buscarOFallar(id);
        return EstadoCuentaResponse.builder()
                .clienteId(id)
                .disponible(false)
                .mensaje("finanzas-service aun no esta disponible")
                .build();
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
