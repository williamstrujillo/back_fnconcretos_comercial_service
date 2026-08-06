package mx.fnconcretos.comercial.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.fnconcretos.comercial.dto.request.VisitaCheckinRequest;
import mx.fnconcretos.comercial.dto.request.VisitaRequest;
import mx.fnconcretos.comercial.dto.response.ResumenVisitasResponse;
import mx.fnconcretos.comercial.dto.response.VisitaResponse;
import mx.fnconcretos.comercial.service.VisitaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/visitas")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "visita-controller", description = "Visitas de asesores a obra, con check-in geolocalizado")
public class VisitaController {

    private final VisitaService visitaService;

    @GetMapping
    @Operation(summary = "Listar las visitas de un asesor en una fecha")
    public List<VisitaResponse> listar(@RequestParam Long asesorId,
                                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return visitaService.listarPorAsesorYFecha(asesorId, fecha);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Asignar una visita a obra a un asesor")
    public VisitaResponse asignar(@Valid @RequestBody VisitaRequest request) {
        return visitaService.asignar(request);
    }

    @PostMapping("/{id}/checkin")
    @Operation(summary = "Registrar el check-in geolocalizado de la visita (QR o app)")
    public VisitaResponse checkin(@PathVariable Long id, @Valid @RequestBody VisitaCheckinRequest request) {
        return visitaService.checkin(id, request);
    }

    @GetMapping("/resumen-dia")
    @Operation(summary = "Resumen de visitas del dia de un asesor frente al minimo diario requerido")
    public ResumenVisitasResponse resumenDelDia(@RequestParam Long asesorId,
                                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return visitaService.resumenDelDia(asesorId, fecha);
    }
}
