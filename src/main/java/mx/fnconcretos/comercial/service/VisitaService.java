package mx.fnconcretos.comercial.service;

import lombok.RequiredArgsConstructor;
import mx.fnconcretos.comercial.dto.request.VisitaCheckinRequest;
import mx.fnconcretos.comercial.dto.request.VisitaRequest;
import mx.fnconcretos.comercial.dto.response.ResumenVisitasResponse;
import mx.fnconcretos.comercial.dto.response.VisitaResponse;
import mx.fnconcretos.comercial.entity.AsesorComercial;
import mx.fnconcretos.comercial.entity.Cliente;
import mx.fnconcretos.comercial.entity.Obra;
import mx.fnconcretos.comercial.entity.VisitaObra;
import mx.fnconcretos.comercial.exception.EstadoInvalidoException;
import mx.fnconcretos.comercial.exception.ResourceNotFoundException;
import mx.fnconcretos.comercial.repository.VisitaObraRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VisitaService {

    private final VisitaObraRepository visitaRepository;
    private final AsesorComercialService asesorService;
    private final ClienteService clienteService;
    private final ObraService obraService;

    @Value("${negocio.visitas.minimo-diario}")
    private long visitasMinimoDiario;

    @Transactional(readOnly = true)
    public List<VisitaResponse> listarPorAsesorYFecha(Long asesorId, LocalDate fecha) {
        return visitaRepository.findByAsesorIdAndFechaVisita(asesorId, fecha).stream().map(this::toResponse).toList();
    }

    @Transactional
    public VisitaResponse asignar(VisitaRequest request) {
        AsesorComercial asesor = asesorService.buscarOFallar(request.getAsesorId());
        Obra obra = request.getObraId() != null ? obraService.buscarOFallar(request.getObraId()) : null;
        Cliente cliente = request.getClienteId() != null ? clienteService.buscarOFallar(request.getClienteId()) : null;

        VisitaObra visita = VisitaObra.builder()
                .asesor(asesor)
                .obra(obra)
                .cliente(cliente)
                .zonaId(request.getZonaId())
                .fechaVisita(request.getFechaVisita())
                .contactoNombre(request.getContactoNombre())
                .contactoTelefono(request.getContactoTelefono())
                .volumenAproximado(request.getVolumenAproximado())
                .build();

        return toResponse(visitaRepository.save(visita));
    }

    @Transactional
    public VisitaResponse checkin(Long id, VisitaCheckinRequest request) {
        VisitaObra visita = buscarOFallar(id);
        if ("visitada".equals(visita.getEstatus())) {
            throw new EstadoInvalidoException("La visita " + id + " ya tiene checkin registrado");
        }

        visita.setLatitud(request.getLatitud());
        visita.setLongitud(request.getLongitud());
        visita.setHoraCheckin(LocalTime.now());
        if (request.getFotoEvidenciaUrl() != null) visita.setFotoEvidenciaUrl(request.getFotoEvidenciaUrl());
        if (request.getContactoNombre() != null) visita.setContactoNombre(request.getContactoNombre());
        if (request.getContactoTelefono() != null) visita.setContactoTelefono(request.getContactoTelefono());
        if (request.getMetodoCheckin() != null) visita.setMetodoCheckin(request.getMetodoCheckin());
        visita.setEstatus("visitada");

        return toResponse(visitaRepository.save(visita));
    }

    @Transactional(readOnly = true)
    public ResumenVisitasResponse resumenDelDia(Long asesorId, LocalDate fecha) {
        AsesorComercial asesor = asesorService.buscarOFallar(asesorId);

        long realizadas = visitaRepository.findByAsesorIdAndFechaVisita(asesorId, fecha).stream()
                .filter(v -> "visitada".equals(v.getEstatus()))
                .count();

        return ResumenVisitasResponse.builder()
                .asesorId(asesor.getId())
                .asesorNombre(asesor.getNombre())
                .fecha(fecha)
                .visitasRealizadas(realizadas)
                .visitasMinimoRequerido(visitasMinimoDiario)
                .cumpleMinimo(realizadas >= visitasMinimoDiario)
                .build();
    }

    private VisitaObra buscarOFallar(Long id) {
        return visitaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Visita no encontrada: " + id));
    }

    private VisitaResponse toResponse(VisitaObra visita) {
        return VisitaResponse.builder()
                .id(visita.getId())
                .asesorId(visita.getAsesor().getId())
                .asesorNombre(visita.getAsesor().getNombre())
                .obraId(visita.getObra() != null ? visita.getObra().getId() : null)
                .obraNombre(visita.getObra() != null ? visita.getObra().getNombre() : null)
                .clienteId(visita.getCliente() != null ? visita.getCliente().getId() : null)
                .clienteNombre(visita.getCliente() != null ? visita.getCliente().getNombre() : null)
                .zonaId(visita.getZonaId())
                .fechaVisita(visita.getFechaVisita())
                .horaCheckin(visita.getHoraCheckin())
                .latitud(visita.getLatitud())
                .longitud(visita.getLongitud())
                .fotoEvidenciaUrl(visita.getFotoEvidenciaUrl())
                .contactoNombre(visita.getContactoNombre())
                .contactoTelefono(visita.getContactoTelefono())
                .volumenAproximado(visita.getVolumenAproximado())
                .metodoCheckin(visita.getMetodoCheckin())
                .estatus(visita.getEstatus())
                .createdAt(visita.getCreatedAt())
                .build();
    }
}
