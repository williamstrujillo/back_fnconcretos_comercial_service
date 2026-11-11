package mx.fnconcretos.comercial.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.fnconcretos.comercial.dto.request.EstatusRequest;
import mx.fnconcretos.comercial.dto.request.ObraRequest;
import mx.fnconcretos.comercial.dto.request.VincularClienteRequest;
import mx.fnconcretos.comercial.dto.response.ClienteObraResponse;
import mx.fnconcretos.comercial.dto.response.ObraResponse;
import mx.fnconcretos.comercial.security.JwtPrincipal;
import mx.fnconcretos.comercial.service.ObraService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/obras")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "obra-controller", description = "Obras/proyectos de construccion y su ubicacion (recibida por WhatsApp via Google Maps)")
public class ObraController {

    private final ObraService obraService;

    @GetMapping
    @Operation(summary = "Buscar obras por estatus, ciudad o nombre. Un Asesor Comercial solo ve las obras de "
            + "sus propios clientes.")
    public List<ObraResponse> listar(@RequestParam(required = false) String estatus,
                                      @RequestParam(required = false) String ciudad,
                                      @RequestParam(required = false) String q,
                                      @AuthenticationPrincipal JwtPrincipal principal) {
        return obraService.listar(estatus, ciudad, q, principal);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Alta de obra. Un Asesor Comercial solo puede darla de alta para un cliente propio.")
    public ObraResponse crear(@Valid @RequestBody ObraRequest request, @AuthenticationPrincipal JwtPrincipal principal) {
        return obraService.crear(request, principal);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener detalle de una obra. Un Asesor Comercial solo puede ver las de sus propios "
            + "clientes (404 si no le pertenece).")
    public ObraResponse obtener(@PathVariable Long id, @AuthenticationPrincipal JwtPrincipal principal) {
        return obraService.obtener(id, principal);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar datos de una obra")
    public ObraResponse actualizar(@PathVariable Long id, @Valid @RequestBody ObraRequest request,
                                    @AuthenticationPrincipal JwtPrincipal principal) {
        return obraService.actualizar(id, request, principal);
    }

    @PatchMapping("/{id}/estatus")
    @Operation(summary = "Activar/desactivar una obra")
    public ObraResponse cambiarEstatus(@PathVariable Long id, @Valid @RequestBody EstatusRequest request,
                                        @AuthenticationPrincipal JwtPrincipal principal) {
        return obraService.cambiarEstatus(id, request, principal);
    }

    @PostMapping("/{id}/clientes")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Vincular un cliente adicional a la obra (una obra puede tener mas de un cliente comprando concreto)")
    public ClienteObraResponse vincularCliente(@PathVariable Long id, @Valid @RequestBody VincularClienteRequest request) {
        return obraService.vincularCliente(id, request);
    }

    @GetMapping("/{id}/clientes")
    @Operation(summary = "Listar los clientes vinculados a la obra")
    public List<ClienteObraResponse> listarClientesVinculados(@PathVariable Long id) {
        return obraService.listarClientesVinculados(id);
    }

    @DeleteMapping("/{id}/clientes/{clienteId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Desvincular un cliente de la obra")
    public void desvincularCliente(@PathVariable Long id, @PathVariable Long clienteId) {
        obraService.desvincularCliente(id, clienteId);
    }
}
