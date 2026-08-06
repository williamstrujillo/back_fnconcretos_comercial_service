package mx.fnconcretos.comercial.service;

import lombok.RequiredArgsConstructor;
import mx.fnconcretos.comercial.dto.request.AsesorRequest;
import mx.fnconcretos.comercial.dto.request.EstatusRequest;
import mx.fnconcretos.comercial.dto.response.AsesorResponse;
import mx.fnconcretos.comercial.dto.response.DesempenoAsesorResponse;
import mx.fnconcretos.comercial.entity.AsesorComercial;
import mx.fnconcretos.comercial.entity.Cotizacion;
import mx.fnconcretos.comercial.exception.ResourceNotFoundException;
import mx.fnconcretos.comercial.repository.AsesorComercialRepository;
import mx.fnconcretos.comercial.repository.CotizacionRepository;
import mx.fnconcretos.comercial.repository.VisitaObraRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AsesorComercialService {

    private final AsesorComercialRepository asesorRepository;
    private final CotizacionRepository cotizacionRepository;
    private final VisitaObraRepository visitaRepository;

    @Transactional(readOnly = true)
    public List<AsesorResponse> listar() {
        return asesorRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public AsesorResponse crear(AsesorRequest request) {
        AsesorComercial asesor = AsesorComercial.builder()
                .usuarioId(request.getUsuarioId())
                .nombre(request.getNombre())
                .tipo(request.getTipo() != null ? request.getTipo() : "asesor")
                .porcentajeComisionDefault(request.getPorcentajeComisionDefault() != null
                        ? request.getPorcentajeComisionDefault() : new BigDecimal("2.00"))
                .plantaId(request.getPlantaId())
                .build();
        return toResponse(asesorRepository.save(asesor));
    }

    @Transactional(readOnly = true)
    public AsesorResponse obtener(Long id) {
        return toResponse(buscarOFallar(id));
    }

    @Transactional
    public AsesorResponse actualizar(Long id, AsesorRequest request) {
        AsesorComercial asesor = buscarOFallar(id);

        asesor.setUsuarioId(request.getUsuarioId());
        asesor.setNombre(request.getNombre());
        if (request.getTipo() != null) asesor.setTipo(request.getTipo());
        if (request.getPorcentajeComisionDefault() != null) asesor.setPorcentajeComisionDefault(request.getPorcentajeComisionDefault());
        asesor.setPlantaId(request.getPlantaId());

        return toResponse(asesorRepository.save(asesor));
    }

    @Transactional
    public AsesorResponse cambiarEstatus(Long id, EstatusRequest request) {
        AsesorComercial asesor = buscarOFallar(id);
        asesor.setEstatus(request.getEstatus());
        return toResponse(asesorRepository.save(asesor));
    }

    @Transactional(readOnly = true)
    public DesempenoAsesorResponse desempeno(Long id, LocalDate desde, LocalDate hasta) {
        AsesorComercial asesor = buscarOFallar(id);

        List<Cotizacion> cotizaciones = cotizacionRepository.findAll().stream()
                .filter(c -> c.getAsesor() != null && c.getAsesor().getId().equals(id))
                .filter(c -> !c.getCreatedAt().toLocalDate().isBefore(desde) && !c.getCreatedAt().toLocalDate().isAfter(hasta))
                .toList();

        long generadas = cotizaciones.size();
        long convertidas = cotizaciones.stream().filter(c -> "convertida".equals(c.getEstatus())).count();
        long visitas = visitaRepository.findAll().stream()
                .filter(v -> v.getAsesor() != null && v.getAsesor().getId().equals(id))
                .filter(v -> !v.getFechaVisita().isBefore(desde) && !v.getFechaVisita().isAfter(hasta))
                .filter(v -> "visitada".equals(v.getEstatus()))
                .count();

        double tasaConversion = generadas > 0
                ? BigDecimal.valueOf(convertidas).multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(generadas), 2, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

        return DesempenoAsesorResponse.builder()
                .asesorId(asesor.getId())
                .asesorNombre(asesor.getNombre())
                .desde(desde)
                .hasta(hasta)
                .obrasVisitadas(visitas)
                .cotizacionesGeneradas(generadas)
                .cotizacionesConvertidas(convertidas)
                .tasaConversionPct(tasaConversion)
                .build();
    }

    protected AsesorComercial buscarOFallar(Long id) {
        return asesorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asesor no encontrado: " + id));
    }

    private AsesorResponse toResponse(AsesorComercial asesor) {
        return AsesorResponse.builder()
                .id(asesor.getId())
                .usuarioId(asesor.getUsuarioId())
                .nombre(asesor.getNombre())
                .tipo(asesor.getTipo())
                .porcentajeComisionDefault(asesor.getPorcentajeComisionDefault())
                .plantaId(asesor.getPlantaId())
                .estatus(asesor.getEstatus())
                .createdAt(asesor.getCreatedAt())
                .build();
    }
}
