package mx.fnconcretos.comercial.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.fnconcretos.comercial.dto.request.AjustarVolumenPedidoRequest;
import mx.fnconcretos.comercial.dto.request.CambiarProductoPedidoRequest;
import mx.fnconcretos.comercial.dto.request.PedidoUpdateRequest;
import mx.fnconcretos.comercial.dto.request.RegistrarEntregaRequest;
import mx.fnconcretos.comercial.dto.response.AvancePedidoResponse;
import mx.fnconcretos.comercial.dto.response.BitacoraEventoResponse;
import mx.fnconcretos.comercial.dto.response.PedidoResponse;
import mx.fnconcretos.comercial.dto.response.WhatsAppEnvioResponse;
import mx.fnconcretos.comercial.security.JwtPrincipal;
import mx.fnconcretos.comercial.service.BitacoraService;
import mx.fnconcretos.comercial.service.PedidoService;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
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
    @Operation(summary = "Buscar pedidos por cliente, planta, asesor, estatus general o folio (q, coincidencia parcial). "
            + "desde/hasta filtran por fechaProgramada (para el informe de pedidos y ventas por periodo de Direccion).")
    public List<PedidoResponse> listar(@RequestParam(required = false) Long clienteId,
                                        @RequestParam(required = false) String estatusGeneral,
                                        @RequestParam(required = false) String q,
                                        @RequestParam(required = false) Long plantaId,
                                        @RequestParam(required = false) Long asesorId,
                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                        @AuthenticationPrincipal JwtPrincipal principal) {
        return pedidoService.listar(clienteId, estatusGeneral, q, plantaId, asesorId, desde, hasta, principal);
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

    @PatchMapping("/{id}/detalle/{detalleId}/ajustar-volumen")
    @Operation(summary = "Ajustar el volumen (y por lo tanto el precioTotal) de una linea de producto del pedido -- "
            + "ej. el cliente pide mas o menos m3 en la ultima remision que aun va a salir. Nunca permite bajar de "
            + "lo ya entregado. No vuelve a pedir autorizacion de pago aunque el monto suba.")
    public PedidoResponse ajustarVolumen(@PathVariable Long id, @PathVariable Long detalleId,
                                          @Valid @RequestBody AjustarVolumenPedidoRequest request,
                                          @AuthenticationPrincipal JwtPrincipal principal) {
        return pedidoService.ajustarVolumen(id, detalleId, request, principal);
    }

    @PatchMapping("/{id}/detalle/{detalleId}/cambiar-producto")
    @Operation(summary = "Cambiar el producto (ej. resistencia) de una linea del pedido -- cada resistencia es un "
            + "producto distinto en el catalogo. Solo permitido mientras la linea no tenga nada entregado "
            + "(antes de que salga la primera remision).")
    public PedidoResponse cambiarProducto(@PathVariable Long id, @PathVariable Long detalleId,
                                           @Valid @RequestBody CambiarProductoPedidoRequest request,
                                           @AuthenticationPrincipal JwtPrincipal principal) {
        return pedidoService.cambiarProducto(id, detalleId, request, principal);
    }

    @PostMapping("/{id}/enviar-whatsapp-seguimiento")
    @Operation(summary = "Enviar el link de seguimiento del pedido al cliente por WhatsApp",
            description = "Usa la plantilla aprobada 'pedido_seguimiento' (con boton de rastreo) via la API de "
                    + "WhatsApp Business -- ya no abre un link wa.me manual. Reutiliza/genera el mismo token "
                    + "publico que 'Compartir seguimiento'. 409 si el cliente no tiene telefono o si Meta rechaza "
                    + "el envio (plantilla pendiente de aprobacion, numero no autorizado, etc).")
    public WhatsAppEnvioResponse enviarWhatsAppSeguimiento(@PathVariable Long id, @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return pedidoService.enviarWhatsAppSeguimiento(id, authorization);
    }

    @GetMapping("/{id}/bitacora")
    @Operation(summary = "Historial completo de acciones sobre el pedido (quien y cuando), en orden mas reciente primero")
    public List<BitacoraEventoResponse> bitacora(@PathVariable Long id) {
        return bitacoraService.listar("pedido", id);
    }
}
