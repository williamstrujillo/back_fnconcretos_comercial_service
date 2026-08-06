package mx.fnconcretos.comercial.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.fnconcretos.comercial.dto.request.SolicitudDisenoRequest;
import mx.fnconcretos.comercial.dto.request.ViabilidadDisenoRequest;
import mx.fnconcretos.comercial.dto.response.SolicitudDisenoResponse;
import mx.fnconcretos.comercial.security.JwtPrincipal;
import mx.fnconcretos.comercial.service.SolicitudDisenoService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/solicitudes-diseno")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "solicitud-diseno-controller", description = "Solicitudes de diseno especial de mezcla, resueltas por Laboratorio")
public class SolicitudDisenoController {

    private final SolicitudDisenoService solicitudDisenoService;

    @GetMapping
    @Operation(summary = "Buscar solicitudes de diseno por estatus")
    public List<SolicitudDisenoResponse> listar(@RequestParam(required = false) String estatus) {
        return solicitudDisenoService.listar(estatus);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Alta de solicitud de diseno especial")
    public SolicitudDisenoResponse crear(@Valid @RequestBody SolicitudDisenoRequest request) {
        return solicitudDisenoService.crear(request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener detalle de una solicitud de diseno")
    public SolicitudDisenoResponse obtener(@PathVariable Long id) {
        return solicitudDisenoService.obtener(id);
    }

    @PatchMapping("/{id}/viabilidad")
    @Operation(summary = "Registrar el resultado de viabilidad (Laboratorio)")
    public SolicitudDisenoResponse resolverViabilidad(@PathVariable Long id, @Valid @RequestBody ViabilidadDisenoRequest request,
                                                        @AuthenticationPrincipal JwtPrincipal principal) {
        return solicitudDisenoService.resolverViabilidad(id, request, principal != null ? principal.usuarioId() : null);
    }
}
