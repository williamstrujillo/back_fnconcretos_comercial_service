package mx.fnconcretos.comercial.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.fnconcretos.comercial.dto.request.AutorizarRequest;
import mx.fnconcretos.comercial.dto.response.AutorizacionResponse;
import mx.fnconcretos.comercial.security.JwtPrincipal;
import mx.fnconcretos.comercial.service.AutorizacionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/pedidos/{pedidoId}/autorizaciones")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "autorizacion-controller", description = "Autorizacion de credito/pago y de logistica sobre un pedido")
public class AutorizacionController {

    private final AutorizacionService autorizacionService;

    @GetMapping
    @Operation(summary = "Listar las autorizaciones registradas para un pedido")
    public List<AutorizacionResponse> listar(@PathVariable Long pedidoId) {
        return autorizacionService.listarPorPedido(pedidoId);
    }

    @PostMapping("/pago")
    @PreAuthorize("hasAuthority('pedidos.autorizar_credito')")
    @Operation(summary = "Autorizar o rechazar el pago/credito del pedido; requiere permiso pedidos.autorizar_credito")
    public AutorizacionResponse autorizarPago(@PathVariable Long pedidoId, @Valid @RequestBody AutorizarRequest request,
                                               @AuthenticationPrincipal JwtPrincipal principal) {
        return autorizacionService.autorizarPago(pedidoId, request, principal.usuarioId());
    }

    @PostMapping("/logistica")
    @PreAuthorize("hasAuthority('pedidos.autorizar_logistica')")
    @Operation(summary = "Autorizar o rechazar la logistica del pedido; requiere permiso pedidos.autorizar_logistica")
    public AutorizacionResponse autorizarLogistica(@PathVariable Long pedidoId, @Valid @RequestBody AutorizarRequest request,
                                                    @AuthenticationPrincipal JwtPrincipal principal) {
        return autorizacionService.autorizarLogistica(pedidoId, request, principal.usuarioId());
    }
}
