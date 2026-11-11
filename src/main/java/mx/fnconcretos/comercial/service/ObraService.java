package mx.fnconcretos.comercial.service;

import lombok.RequiredArgsConstructor;
import mx.fnconcretos.comercial.dto.request.EstatusRequest;
import mx.fnconcretos.comercial.dto.request.ObraRequest;
import mx.fnconcretos.comercial.dto.request.VincularClienteRequest;
import mx.fnconcretos.comercial.dto.response.ClienteObraResponse;
import mx.fnconcretos.comercial.dto.response.ObraResponse;
import mx.fnconcretos.comercial.entity.AsesorComercial;
import mx.fnconcretos.comercial.entity.Cliente;
import mx.fnconcretos.comercial.entity.ClienteObra;
import mx.fnconcretos.comercial.entity.ContactoCliente;
import mx.fnconcretos.comercial.entity.Obra;
import mx.fnconcretos.comercial.exception.ConflictException;
import mx.fnconcretos.comercial.exception.ResourceNotFoundException;
import mx.fnconcretos.comercial.repository.AsesorComercialRepository;
import mx.fnconcretos.comercial.repository.ClienteObraRepository;
import mx.fnconcretos.comercial.repository.ContactoClienteRepository;
import mx.fnconcretos.comercial.repository.ObraRepository;
import mx.fnconcretos.comercial.security.JwtPrincipal;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ObraService {

    private static final Pattern COORDS_EN_LINK = Pattern.compile("(-?\\d{1,3}\\.\\d+),\\s*(-?\\d{1,3}\\.\\d+)");

    private final ObraRepository obraRepository;
    private final ClienteObraRepository clienteObraRepository;
    private final ContactoClienteRepository contactoClienteRepository;
    private final ClienteService clienteService;
    private final AsesorComercialRepository asesorRepository;

    @Transactional(readOnly = true)
    public List<ObraResponse> listar(String estatus, String ciudad, String q, JwtPrincipal principal) {
        Specification<Obra> spec = Specification.where(null);

        if (estatus != null && !estatus.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("estatus"), estatus));
        }
        if (ciudad != null && !ciudad.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("ciudad"), ciudad));
        }
        if (q != null && !q.isBlank()) {
            String like = "%" + q.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("nombre")), like));
        }
        if (esAsesorComercial(principal)) {
            Long miAsesorId = miAsesor(principal).getId();
            spec = spec.and((root, query, cb) -> cb.equal(root.get("clientePrincipal").get("asesorAsignado").get("id"), miAsesorId));
        }

        return obraRepository.findAll(spec).stream().map(this::toResponse).toList();
    }

    @Transactional
    public ObraResponse crear(ObraRequest request, JwtPrincipal principal) {
        Cliente clientePrincipal = request.getClientePrincipalId() != null
                ? clienteService.buscarOFallar(request.getClientePrincipalId())
                : null;

        // Un Asesor Comercial solo puede dar de alta obras de un cliente propio -- la obra no
        // tiene su propio campo de asesor, se hereda del cliente principal.
        if (esAsesorComercial(principal)) {
            Long miAsesorId = miAsesor(principal).getId();
            boolean esDeMiCliente = clientePrincipal != null && clientePrincipal.getAsesorAsignado() != null
                    && clientePrincipal.getAsesorAsignado().getId().equals(miAsesorId);
            if (!esDeMiCliente) {
                throw new ResourceNotFoundException("Cliente no encontrado: " + request.getClientePrincipalId());
            }
        }

        BigDecimal[] coords = extraerCoordenadas(request.getGoogleMapsLink());

        Obra obra = Obra.builder()
                .nombre(request.getNombre())
                .clientePrincipal(clientePrincipal)
                .direccion(request.getDireccion())
                .googleMapsLink(request.getGoogleMapsLink())
                .latitud(request.getLatitud() != null ? request.getLatitud() : coords[0])
                .longitud(request.getLongitud() != null ? request.getLongitud() : coords[1])
                .ciudad(request.getCiudad())
                .colonia(request.getColonia())
                .plantaId(request.getPlantaId())
                .build();

        Obra guardada = obraRepository.save(obra);

        if (clientePrincipal != null) {
            clienteObraRepository.save(ClienteObra.builder()
                    .cliente(clientePrincipal)
                    .obra(guardada)
                    .build());
        }

        return toResponse(guardada);
    }

    @Transactional(readOnly = true)
    public ObraResponse obtener(Long id, JwtPrincipal principal) {
        return toResponse(buscarPropiaOFallar(id, principal));
    }

    @Transactional
    public ObraResponse actualizar(Long id, ObraRequest request, JwtPrincipal principal) {
        Obra obra = buscarPropiaOFallar(id, principal);

        obra.setNombre(request.getNombre());
        if (request.getClientePrincipalId() != null) {
            Cliente nuevoClientePrincipal = clienteService.buscarOFallar(request.getClientePrincipalId());
            if (esAsesorComercial(principal)) {
                Long miAsesorId = miAsesor(principal).getId();
                boolean esDeMiCliente = nuevoClientePrincipal.getAsesorAsignado() != null
                        && nuevoClientePrincipal.getAsesorAsignado().getId().equals(miAsesorId);
                if (!esDeMiCliente) {
                    throw new ResourceNotFoundException("Cliente no encontrado: " + request.getClientePrincipalId());
                }
            }
            obra.setClientePrincipal(nuevoClientePrincipal);
        }
        obra.setDireccion(request.getDireccion());

        if (request.getGoogleMapsLink() != null && !request.getGoogleMapsLink().equals(obra.getGoogleMapsLink())) {
            BigDecimal[] coords = extraerCoordenadas(request.getGoogleMapsLink());
            obra.setGoogleMapsLink(request.getGoogleMapsLink());
            if (request.getLatitud() == null) obra.setLatitud(coords[0]);
            if (request.getLongitud() == null) obra.setLongitud(coords[1]);
        }
        if (request.getLatitud() != null) obra.setLatitud(request.getLatitud());
        if (request.getLongitud() != null) obra.setLongitud(request.getLongitud());

        obra.setCiudad(request.getCiudad());
        obra.setColonia(request.getColonia());
        obra.setPlantaId(request.getPlantaId());

        return toResponse(obraRepository.save(obra));
    }

    @Transactional
    public ObraResponse cambiarEstatus(Long id, EstatusRequest request, JwtPrincipal principal) {
        Obra obra = buscarPropiaOFallar(id, principal);
        obra.setEstatus(request.getEstatus());
        return toResponse(obraRepository.save(obra));
    }

    @Transactional
    public ClienteObraResponse vincularCliente(Long obraId, VincularClienteRequest request) {
        Obra obra = buscarOFallar(obraId);

        if (clienteObraRepository.existsByClienteIdAndObraId(request.getClienteId(), obraId)) {
            throw new ConflictException("El cliente " + request.getClienteId() + " ya esta vinculado a la obra " + obraId);
        }

        Cliente cliente = clienteService.buscarOFallar(request.getClienteId());
        ContactoCliente contacto = null;
        if (request.getContactoId() != null) {
            contacto = contactoClienteRepository.findById(request.getContactoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Contacto no encontrado: " + request.getContactoId()));
        }

        ClienteObra vinculo = ClienteObra.builder()
                .cliente(cliente)
                .obra(obra)
                .contacto(contacto)
                .build();

        return toClienteObraResponse(clienteObraRepository.save(vinculo));
    }

    @Transactional(readOnly = true)
    public List<ClienteObraResponse> listarClientesVinculados(Long obraId) {
        buscarOFallar(obraId);
        return clienteObraRepository.findByObraId(obraId).stream().map(this::toClienteObraResponse).toList();
    }

    @Transactional
    public void desvincularCliente(Long obraId, Long clienteId) {
        ClienteObra vinculo = clienteObraRepository.findByClienteIdAndObraId(clienteId, obraId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El cliente " + clienteId + " no esta vinculado a la obra " + obraId));
        clienteObraRepository.delete(vinculo);
    }

    protected Obra buscarOFallar(Long id) {
        return obraRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Obra no encontrada: " + id));
    }

    /** Igual que buscarOFallar, pero valida que la obra sea de un cliente propio cuando quien pide
     * es un Asesor Comercial (la obra hereda el "dueno" de su cliente principal, no tiene asesor
     * propio) -- 404 en vez de 403 para no revelar que la obra existe. */
    private Obra buscarPropiaOFallar(Long id, JwtPrincipal principal) {
        Obra obra = buscarOFallar(id);
        if (esAsesorComercial(principal)) {
            Long miAsesorId = miAsesor(principal).getId();
            Cliente clientePrincipal = obra.getClientePrincipal();
            boolean esDeMiCliente = clientePrincipal != null && clientePrincipal.getAsesorAsignado() != null
                    && clientePrincipal.getAsesorAsignado().getId().equals(miAsesorId);
            if (!esDeMiCliente) {
                throw new ResourceNotFoundException("Obra no encontrada: " + id);
            }
        }
        return obra;
    }

    private boolean esAsesorComercial(JwtPrincipal principal) {
        return principal != null && "Asesor Comercial".equals(principal.rol());
    }

    private AsesorComercial miAsesor(JwtPrincipal principal) {
        return asesorRepository.findByUsuarioId(principal.usuarioId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El usuario " + principal.usuarioId() + " no tiene un asesor comercial asociado"));
    }

    private BigDecimal[] extraerCoordenadas(String googleMapsLink) {
        if (googleMapsLink == null || googleMapsLink.isBlank()) {
            return new BigDecimal[]{null, null};
        }
        Matcher matcher = COORDS_EN_LINK.matcher(googleMapsLink);
        if (matcher.find()) {
            return new BigDecimal[]{new BigDecimal(matcher.group(1)), new BigDecimal(matcher.group(2))};
        }
        return new BigDecimal[]{null, null};
    }

    ObraResponse toResponse(Obra obra) {
        return ObraResponse.builder()
                .id(obra.getId())
                .nombre(obra.getNombre())
                .clientePrincipalId(obra.getClientePrincipal() != null ? obra.getClientePrincipal().getId() : null)
                .clientePrincipalNombre(obra.getClientePrincipal() != null ? obra.getClientePrincipal().getNombre() : null)
                .direccion(obra.getDireccion())
                .googleMapsLink(obra.getGoogleMapsLink())
                .latitud(obra.getLatitud())
                .longitud(obra.getLongitud())
                .ciudad(obra.getCiudad())
                .colonia(obra.getColonia())
                .plantaId(obra.getPlantaId())
                .estatus(obra.getEstatus())
                .createdAt(obra.getCreatedAt())
                .build();
    }

    private ClienteObraResponse toClienteObraResponse(ClienteObra vinculo) {
        return ClienteObraResponse.builder()
                .id(vinculo.getId())
                .clienteId(vinculo.getCliente().getId())
                .clienteNombre(vinculo.getCliente().getNombre())
                .obraId(vinculo.getObra().getId())
                .obraNombre(vinculo.getObra().getNombre())
                .contactoId(vinculo.getContacto() != null ? vinculo.getContacto().getId() : null)
                .contactoNombre(vinculo.getContacto() != null ? vinculo.getContacto().getNombre() : null)
                .fechaAsociacion(vinculo.getFechaAsociacion())
                .build();
    }
}
