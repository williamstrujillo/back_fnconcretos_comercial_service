package mx.fnconcretos.comercial.service;

import lombok.RequiredArgsConstructor;
import mx.fnconcretos.comercial.dto.request.SolicitudDisenoRequest;
import mx.fnconcretos.comercial.dto.request.ViabilidadDisenoRequest;
import mx.fnconcretos.comercial.dto.response.SolicitudDisenoResponse;
import mx.fnconcretos.comercial.entity.Cliente;
import mx.fnconcretos.comercial.entity.Obra;
import mx.fnconcretos.comercial.entity.SolicitudDisenoEspecial;
import mx.fnconcretos.comercial.exception.EstadoInvalidoException;
import mx.fnconcretos.comercial.exception.ResourceNotFoundException;
import mx.fnconcretos.comercial.repository.SolicitudDisenoEspecialRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SolicitudDisenoService {

    private final SolicitudDisenoEspecialRepository solicitudRepository;
    private final ClienteService clienteService;
    private final ObraService obraService;

    @Transactional(readOnly = true)
    public List<SolicitudDisenoResponse> listar(String estatus) {
        List<SolicitudDisenoEspecial> solicitudes = estatus != null && !estatus.isBlank()
                ? solicitudRepository.findAll().stream().filter(s -> estatus.equals(s.getEstatus())).toList()
                : solicitudRepository.findAll();
        return solicitudes.stream().map(this::toResponse).toList();
    }

    @Transactional
    public SolicitudDisenoResponse crear(SolicitudDisenoRequest request) {
        Cliente cliente = clienteService.buscarOFallar(request.getClienteId());
        Obra obra = request.getObraId() != null ? obraService.buscarOFallar(request.getObraId()) : null;

        SolicitudDisenoEspecial solicitud = SolicitudDisenoEspecial.builder()
                .cliente(cliente)
                .obra(obra)
                .productoSolicitado(request.getProductoSolicitado())
                .revenimiento(request.getRevenimiento())
                .tamanoAgregado(request.getTamanoAgregado())
                .caracteristicaEspecial(request.getCaracteristicaEspecial())
                .aditivosRequeridos(request.getAditivosRequeridos())
                .fechaDeseada(request.getFechaDeseada())
                .fechaLimiteRespuesta(request.getFechaLimiteRespuesta())
                .insumosRequeridos(request.getInsumosRequeridos())
                .evidenciaUrl(request.getEvidenciaUrl())
                .build();

        return toResponse(solicitudRepository.save(solicitud));
    }

    @Transactional(readOnly = true)
    public SolicitudDisenoResponse obtener(Long id) {
        return toResponse(buscarOFallar(id));
    }

    @Transactional
    public SolicitudDisenoResponse resolverViabilidad(Long id, ViabilidadDisenoRequest request, Long responsableLaboratorioId) {
        SolicitudDisenoEspecial solicitud = buscarOFallar(id);
        if (!"recibida".equals(solicitud.getEstatus()) && !"en_analisis".equals(solicitud.getEstatus())) {
            throw new EstadoInvalidoException("La solicitud " + id + " ya fue resuelta (estatus actual: " + solicitud.getEstatus() + ")");
        }

        solicitud.setResultadoViabilidad(request.getResultadoViabilidad());
        solicitud.setObservaciones(request.getObservaciones());
        solicitud.setResponsableLaboratorioId(responsableLaboratorioId);
        solicitud.setEstatus("viable".equals(request.getResultadoViabilidad()) ? "resuelta" : "rechazada");

        return toResponse(solicitudRepository.save(solicitud));
    }

    private SolicitudDisenoEspecial buscarOFallar(Long id) {
        return solicitudRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud de diseno no encontrada: " + id));
    }

    private SolicitudDisenoResponse toResponse(SolicitudDisenoEspecial solicitud) {
        return SolicitudDisenoResponse.builder()
                .id(solicitud.getId())
                .clienteId(solicitud.getCliente().getId())
                .clienteNombre(solicitud.getCliente().getNombre())
                .obraId(solicitud.getObra() != null ? solicitud.getObra().getId() : null)
                .obraNombre(solicitud.getObra() != null ? solicitud.getObra().getNombre() : null)
                .productoSolicitado(solicitud.getProductoSolicitado())
                .revenimiento(solicitud.getRevenimiento())
                .tamanoAgregado(solicitud.getTamanoAgregado())
                .caracteristicaEspecial(solicitud.getCaracteristicaEspecial())
                .aditivosRequeridos(solicitud.getAditivosRequeridos())
                .fechaDeseada(solicitud.getFechaDeseada())
                .fechaLimiteRespuesta(solicitud.getFechaLimiteRespuesta())
                .insumosRequeridos(solicitud.getInsumosRequeridos())
                .evidenciaUrl(solicitud.getEvidenciaUrl())
                .resultadoViabilidad(solicitud.getResultadoViabilidad())
                .estatus(solicitud.getEstatus())
                .responsableLaboratorioId(solicitud.getResponsableLaboratorioId())
                .observaciones(solicitud.getObservaciones())
                .createdAt(solicitud.getCreatedAt())
                .build();
    }
}
