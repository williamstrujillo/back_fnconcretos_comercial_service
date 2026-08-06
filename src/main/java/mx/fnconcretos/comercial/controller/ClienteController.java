package mx.fnconcretos.comercial.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.fnconcretos.comercial.dto.request.ClienteRequest;
import mx.fnconcretos.comercial.dto.request.EstatusRequest;
import mx.fnconcretos.comercial.dto.response.ClienteResponse;
import mx.fnconcretos.comercial.dto.response.EstadoCuentaResponse;
import mx.fnconcretos.comercial.service.ClienteService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/clientes")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "cliente-controller", description = "Alta y gestion de clientes (particulares y corporativos)")
public class ClienteController {

    private final ClienteService clienteService;

    @GetMapping
    @Operation(summary = "Buscar clientes por tipo, estatus o nombre/numero de cliente")
    public List<ClienteResponse> listar(@RequestParam(required = false) String tipo,
                                         @RequestParam(required = false) String estatus,
                                         @RequestParam(required = false) String q) {
        return clienteService.listar(tipo, estatus, q);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Alta de cliente")
    public ClienteResponse crear(@Valid @RequestBody ClienteRequest request) {
        return clienteService.crear(request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener detalle de un cliente")
    public ClienteResponse obtener(@PathVariable Long id) {
        return clienteService.obtener(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar datos de un cliente")
    public ClienteResponse actualizar(@PathVariable Long id, @Valid @RequestBody ClienteRequest request) {
        return clienteService.actualizar(id, request);
    }

    @PatchMapping("/{id}/estatus")
    @Operation(summary = "Activar/desactivar un cliente")
    public ClienteResponse cambiarEstatus(@PathVariable Long id, @Valid @RequestBody EstatusRequest request) {
        return clienteService.cambiarEstatus(id, request);
    }

    @GetMapping("/{id}/estado-cuenta")
    @Operation(summary = "Estado de cuenta del cliente (proxy hacia finanzas-service; responde disponible=false mientras ese servicio no exista)")
    public EstadoCuentaResponse estadoCuenta(@PathVariable Long id) {
        return clienteService.estadoCuenta(id);
    }
}
