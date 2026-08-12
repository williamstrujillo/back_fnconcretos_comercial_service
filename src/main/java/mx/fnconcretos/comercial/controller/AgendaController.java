package mx.fnconcretos.comercial.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.fnconcretos.comercial.dto.request.AgendaRequest;
import mx.fnconcretos.comercial.dto.request.EstatusRequest;
import mx.fnconcretos.comercial.dto.response.AgendaResponse;
import mx.fnconcretos.comercial.dto.response.RutaDiariaResponse;
import mx.fnconcretos.comercial.service.AgendaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/agenda")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "agenda-controller", description = "Agenda de actividades de los asesores comerciales: llamadas, visitas y seguimientos")
public class AgendaController {

    private final AgendaService agendaService;

    @GetMapping("/cliente/{clienteId}")
    @Operation(summary = "Historial de actividades de agenda de un cliente")
    public List<AgendaResponse> listarPorCliente(@PathVariable Long clienteId) {
        return agendaService.listarPorCliente(clienteId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('agenda.administrar')")
    @Operation(summary = "Agendar una actividad (llamada, whatsapp, seguimiento, cotizacion, visita, recordatorio); requiere permiso agenda.administrar")
    public AgendaResponse crear(@Valid @RequestBody AgendaRequest request) {
        return agendaService.crear(request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener detalle de una actividad de agenda")
    public AgendaResponse obtener(@PathVariable Long id) {
        return agendaService.obtener(id);
    }

    @PatchMapping("/{id}/estatus")
    @PreAuthorize("hasAuthority('agenda.administrar')")
    @Operation(summary = "Marcar una actividad como completada/cancelada; requiere permiso agenda.administrar")
    public AgendaResponse cambiarEstatus(@PathVariable Long id, @Valid @RequestBody EstatusRequest request) {
        return agendaService.cambiarEstatus(id, request);
    }

    @GetMapping("/ruta-diaria")
    @Operation(summary = "Ruta/agenda del dia de un asesor")
    public RutaDiariaResponse rutaDiaria(@RequestParam Long asesorId,
                                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return agendaService.rutaDiaria(asesorId, fecha);
    }

    @GetMapping("/pendientes-vencidas")
    @Operation(summary = "Actividades pendientes cuya fecha/hora ya paso, para seguimiento de supervisores")
    public List<AgendaResponse> pendientesVencidas() {
        return agendaService.pendientesVencidas();
    }
}
