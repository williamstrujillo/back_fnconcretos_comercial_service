package mx.fnconcretos.comercial.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import mx.fnconcretos.comercial.dto.response.CotizacionPublicaResponse;
import mx.fnconcretos.comercial.dto.response.TokenCompartidoResponse;
import mx.fnconcretos.comercial.service.CotizacionCompartidaService;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "cotizacion-compartida-controller", description = "Link publico de cotizacion para el cliente final: ver/descargar el mismo formato imprimible, sin login")
public class CotizacionCompartidaController {

    private final CotizacionCompartidaService cotizacionCompartidaService;

    @PostMapping("/cotizaciones/{id}/token-compartido")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Generar (o reutilizar) el token publico de una cotizacion, para compartirla por WhatsApp; requiere sesion de empleado")
    public TokenCompartidoResponse obtenerOCrearToken(@PathVariable Long id,
                                                       @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return cotizacionCompartidaService.obtenerOCrearToken(id, authorization);
    }

    @GetMapping("/publico/cotizacion/{token}")
    @Operation(summary = "Consultar la cotizacion compartida con el token publico: mismo formato imprimible congelado al compartirla; sin autenticacion")
    public CotizacionPublicaResponse consultarPorToken(@PathVariable String token) {
        return cotizacionCompartidaService.consultarPorToken(token);
    }
}
