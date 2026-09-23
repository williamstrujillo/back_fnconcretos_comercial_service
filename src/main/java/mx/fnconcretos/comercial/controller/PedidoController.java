package mx.fnconcretos.comercial.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.fnconcretos.comercial.dto.request.PedidoUpdateRequest;
import mx.fnconcretos.comercial.dto.request.RegistrarEntregaRequest;
import mx.fnconcretos.comercial.dto.response.AvancePedidoResponse;
import mx.fnconcretos.comercial.dto.response.BitacoraEventoResponse;
import mx.fnconcretos.comercial.dto.response.PedidoResponse;
import mx.fnconcretos.comercial.security.JwtPrincipal;
import mx.fnconcretos.comercial.service.BitacoraService;
import mx.fnconcretos.comercial.service.PedidoService;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/pedidos")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "pedido-controller", description = "Pedidos formales generados a partir de una cotizacion, sujetos a autorizacion de pago y logistica")
public class PedidoController {

    private final PedidoService pedidoService;
    private final BitacoraService bitacoraService;

    @GetMapping
    @Operation(summary = "Buscar pedidos por cliente, planta, estatus general o folio (q, coincidencia parcial)")
    public List<PedidoResponse> listar(@RequestParam(required = false) Long clienteId,
                                        @RequestParam(required = false) String estatusGeneral,
                                        @RequestParam(required = false) String q,
                                        @RequestParam(required = false) Long plantaId) {
        return pedidoService.listar(clienteId, estatusGeneral, q, plantaId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener detalle de un pedido")
    public PedidoResponse obtener(@PathVariable Long id) {
        return pedidoService.obtener(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un pedido (solo mientras esta pendiente de autorizacion)")
    public PedidoResponse actualizar(@PathVariable Long id, @Valid @RequestBody PedidoUpdateRequest request,
                                      @AuthenticationPrincipal JwtPrincipal principal) {
        return pedidoService.actualizar(id, request, principal);
    }

    @GetMapping("/{id}/avance")
    @Operation(summary = "Avance del pedido: estatus general y autorizaciones registradas")
    public AvancePedidoResponse avance(@PathVariable Long id) {
        return pedidoService.avance(id);
    }

    @PatchMapping("/{id}/registrar-entrega")
    @Operation(summary = "Acumular el volumen entregado de una remision y avanzar estatusGeneral a parcial/completo. "
            + "Uso interno: lo invoca operaciones-service al firmar una remision, no pensado para llamarse desde la UI directamente.")
    public PedidoResponse registrarEntrega(@PathVariable Long id, @Valid @RequestBody RegistrarEntregaRequest request,
                                            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return pedidoService.registrarEntrega(id, request, authorization);
    }

    @GetMapping("/{id}/bitacora")
    @Operation(summary = "Historial completo de acciones sobre el pedido (quien y cuando), en orden mas reciente primero")
    public List<BitacoraEventoResponse> bitacora(@PathVariable Long id) {
        return bitacoraService.listar("pedido", id);
    }
}
