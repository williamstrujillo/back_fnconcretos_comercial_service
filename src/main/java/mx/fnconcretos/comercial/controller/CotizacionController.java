package mx.fnconcretos.comercial.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.fnconcretos.comercial.dto.request.ConvertirPedidoRequest;
import mx.fnconcretos.comercial.dto.request.CotizacionRequest;
import mx.fnconcretos.comercial.dto.request.EstatusRequest;
import mx.fnconcretos.comercial.dto.response.CotizacionResponse;
import mx.fnconcretos.comercial.dto.response.PedidoResponse;
import mx.fnconcretos.comercial.security.JwtPrincipal;
import mx.fnconcretos.comercial.service.CotizacionService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cotizaciones")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "cotizacion-controller", description = "Cotizaciones de concreto: negociacion, duplicado y conversion a pedido")
public class CotizacionController {

    private final CotizacionService cotizacionService;

    @GetMapping
    @Operation(summary = "Buscar cotizaciones por cliente o estatus")
    public List<CotizacionResponse> listar(@RequestParam(required = false) Long clienteId,
                                            @RequestParam(required = false) String estatus) {
        return cotizacionService.listar(clienteId, estatus);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Generar cotizacion; un descuento mayor al limite por forma de pago requiere el permiso cotizaciones.aplicar_descuento_especial")
    public CotizacionResponse crear(@Valid @RequestBody CotizacionRequest request, @AuthenticationPrincipal JwtPrincipal principal,
                                     @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return cotizacionService.crear(request, principal, authorization);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener detalle de una cotizacion")
    public CotizacionResponse obtener(@PathVariable Long id) {
        return cotizacionService.obtener(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar una cotizacion (solo mientras no este convertida a pedido)")
    public CotizacionResponse actualizar(@PathVariable Long id, @Valid @RequestBody CotizacionRequest request,
                                          @AuthenticationPrincipal JwtPrincipal principal,
                                          @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return cotizacionService.actualizar(id, request, principal, authorization);
    }

    @PatchMapping("/{id}/estatus")
    @Operation(summary = "Cambiar estatus de la cotizacion (negociacion, listo, cancelada)")
    public CotizacionResponse cambiarEstatus(@PathVariable Long id, @Valid @RequestBody EstatusRequest request) {
        return cotizacionService.cambiarEstatus(id, request);
    }

    @PostMapping("/{id}/duplicar")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Duplicar una cotizacion existente para negociar una nueva version (queda enlazada como cotizacionOrigen)")
    public CotizacionResponse duplicar(@PathVariable Long id) {
        return cotizacionService.duplicar(id);
    }

    @PostMapping("/{id}/convertir-pedido")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Convertir una cotizacion en estatus 'listo' en un pedido formal")
    public PedidoResponse convertirAPedido(@PathVariable Long id, @Valid @RequestBody ConvertirPedidoRequest request) {
        return cotizacionService.convertirAPedido(id, request);
    }
}
