package mx.fnconcretos.comercial.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.fnconcretos.comercial.dto.request.ContactoRequest;
import mx.fnconcretos.comercial.dto.response.ContactoResponse;
import mx.fnconcretos.comercial.security.JwtPrincipal;
import mx.fnconcretos.comercial.service.ContactoClienteService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/clientes/{clienteId}/contactos")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "contacto-controller", description = "Contactos de obra/compras de cada cliente")
public class ContactoController {

    private final ContactoClienteService contactoService;

    @GetMapping
    @Operation(summary = "Listar contactos de un cliente. Un Asesor Comercial solo puede verlos si el "
            + "cliente es suyo (404 si no).")
    public List<ContactoResponse> listar(@PathVariable Long clienteId, @AuthenticationPrincipal JwtPrincipal principal) {
        return contactoService.listarPorCliente(clienteId, principal);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Alta de contacto para un cliente")
    public ContactoResponse crear(@PathVariable Long clienteId, @Valid @RequestBody ContactoRequest request,
                                   @AuthenticationPrincipal JwtPrincipal principal) {
        return contactoService.crear(clienteId, request, principal);
    }

    @PutMapping("/{contactoId}")
    @Operation(summary = "Actualizar un contacto")
    public ContactoResponse actualizar(@PathVariable Long clienteId, @PathVariable Long contactoId,
                                        @Valid @RequestBody ContactoRequest request,
                                        @AuthenticationPrincipal JwtPrincipal principal) {
        return contactoService.actualizar(clienteId, contactoId, request, principal);
    }

    @DeleteMapping("/{contactoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar un contacto")
    public void eliminar(@PathVariable Long clienteId, @PathVariable Long contactoId,
                          @AuthenticationPrincipal JwtPrincipal principal) {
        contactoService.eliminar(clienteId, contactoId, principal);
    }
}
