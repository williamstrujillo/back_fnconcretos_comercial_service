package mx.fnconcretos.comercial.service;

import lombok.RequiredArgsConstructor;
import mx.fnconcretos.comercial.dto.request.AgendaRequest;
import mx.fnconcretos.comercial.dto.request.EstatusRequest;
import mx.fnconcretos.comercial.dto.response.AgendaResponse;
import mx.fnconcretos.comercial.dto.response.RutaDiariaResponse;
import mx.fnconcretos.comercial.entity.AgendaActividad;
import mx.fnconcretos.comercial.entity.AsesorComercial;
import mx.fnconcretos.comercial.entity.Cliente;
import mx.fnconcretos.comercial.entity.Cotizacion;
import mx.fnconcretos.comercial.entity.Obra;
import mx.fnconcretos.comercial.entity.Pedido;
import mx.fnconcretos.comercial.exception.ResourceNotFoundException;
import mx.fnconcretos.comercial.repository.AgendaActividadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AgendaService {

    private static final List<String> ESTATUS_CERRADOS = List.of("completada", "cancelada");

    private final AgendaActividadRepository agendaRepository;
    private final AsesorComercialService asesorService;
    private final ClienteService clienteService;
    private final ObraService obraService;
    private final CotizacionService cotizacionService;
    private final PedidoService pedidoService;

    @Transactional(readOnly = true)
    public List<AgendaResponse> listarPorCliente(Long clienteId) {
        return agendaRepository.findByClienteIdOrderByFechaHoraDesc(clienteId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public AgendaResponse crear(AgendaRequest request) {
        AsesorComercial asesor = asesorService.buscarOFallar(request.getAsesorId());
        Cliente cliente = request.getClienteId() != null ? clienteService.buscarOFallar(request.getClienteId()) : null;
        Obra obra = request.getObraId() != null ? obraService.buscarOFallar(request.getObraId()) : null;
        Cotizacion cotizacion = request.getCotizacionId() != null ? cotizacionService.buscarOFallar(request.getCotizacionId()) : null;
        Pedido pedido = request.getPedidoId() != null ? pedidoService.buscarOFallar(request.getPedidoId()) : null;

        AgendaActividad actividad = AgendaActividad.builder()
                .asesor(asesor)
                .cliente(cliente)
                .obra(obra)
                .cotizacion(cotizacion)
                .pedido(pedido)
                .tipoActividad(request.getTipoActividad())
                .fechaHora(request.getFechaHora())
                .minutosRecordatorio(request.getMinutosRecordatorio() != null ? request.getMinutosRecordatorio() : 10)
                .observaciones(request.getObservaciones())
                .build();

        return toResponse(agendaRepository.save(actividad));
    }

    @Transactional(readOnly = true)
    public AgendaResponse obtener(Long id) {
        return toResponse(buscarOFallar(id));
    }

    @Transactional
    public AgendaResponse cambiarEstatus(Long id, EstatusRequest request) {
        AgendaActividad actividad = buscarOFallar(id);
        actividad.setEstatus(request.getEstatus());
        return toResponse(agendaRepository.save(actividad));
    }

    @Transactional(readOnly = true)
    public RutaDiariaResponse rutaDiaria(Long asesorId, LocalDate fecha) {
        AsesorComercial asesor = asesorService.buscarOFallar(asesorId);

        LocalDateTime desde = fecha.atStartOfDay();
        LocalDateTime hasta = fecha.atTime(23, 59, 59);

        List<AgendaActividad> actividades = agendaRepository.findByFechaHoraBetweenOrderByFechaHora(desde, hasta).stream()
                .filter(a -> a.getAsesor().getId().equals(asesorId))
                .toList();

        long completadas = actividades.stream().filter(a -> "completada".equals(a.getEstatus())).count();

        return RutaDiariaResponse.builder()
                .asesorId(asesor.getId())
                .asesorNombre(asesor.getNombre())
                .fecha(fecha)
                .totalActividades(actividades.size())
                .actividadesCompletadas(completadas)
                .actividadesPendientes(actividades.size() - completadas)
                .actividades(actividades.stream().map(this::toResponse).toList())
                .build();
    }

    @Transactional(readOnly = true)
    public List<AgendaResponse> pendientesVencidas() {
        return agendaRepository.findByEstatusNotInAndFechaHoraLessThanOrderByFechaHora(ESTATUS_CERRADOS, LocalDateTime.now())
                .stream().map(this::toResponse).toList();
    }

    private AgendaActividad buscarOFallar(Long id) {
        return agendaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Actividad de agenda no encontrada: " + id));
    }

    private AgendaResponse toResponse(AgendaActividad actividad) {
        return AgendaResponse.builder()
                .id(actividad.getId())
                .asesorId(actividad.getAsesor().getId())
                .asesorNombre(actividad.getAsesor().getNombre())
                .clienteId(actividad.getCliente() != null ? actividad.getCliente().getId() : null)
                .clienteNombre(actividad.getCliente() != null ? actividad.getCliente().getNombre() : null)
                .obraId(actividad.getObra() != null ? actividad.getObra().getId() : null)
                .obraNombre(actividad.getObra() != null ? actividad.getObra().getNombre() : null)
                .cotizacionId(actividad.getCotizacion() != null ? actividad.getCotizacion().getId() : null)
                .pedidoId(actividad.getPedido() != null ? actividad.getPedido().getId() : null)
                .tipoActividad(actividad.getTipoActividad())
                .fechaHora(actividad.getFechaHora())
                .minutosRecordatorio(actividad.getMinutosRecordatorio())
                .estatus(actividad.getEstatus())
                .observaciones(actividad.getObservaciones())
                .createdAt(actividad.getCreatedAt())
                .build();
    }
}
