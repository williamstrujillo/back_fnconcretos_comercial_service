package mx.fnconcretos.comercial.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.fnconcretos.comercial.dto.request.AsesorRequest;
import mx.fnconcretos.comercial.dto.request.EstatusRequest;
import mx.fnconcretos.comercial.dto.response.AsesorResponse;
import mx.fnconcretos.comercial.dto.response.DesempenoAsesorResponse;
import mx.fnconcretos.comercial.service.AsesorComercialService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/asesores")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "asesor-controller", description = "Asesores comerciales (corporativos y de campo) y su desempeno")
public class AsesorController {

    private final AsesorComercialService asesorService;

    @GetMapping
    @Operation(summary = "Listar asesores comerciales")
    public List<AsesorResponse> listar() {
        return asesorService.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Alta de asesor comercial")
    public AsesorResponse crear(@Valid @RequestBody AsesorRequest request) {
        return asesorService.crear(request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener detalle de un asesor")
    public AsesorResponse obtener(@PathVariable Long id) {
        return asesorService.obtener(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar datos de un asesor")
    public AsesorResponse actualizar(@PathVariable Long id, @Valid @RequestBody AsesorRequest request) {
        return asesorService.actualizar(id, request);
    }

    @PatchMapping("/{id}/estatus")
    @Operation(summary = "Activar/desactivar un asesor")
    public AsesorResponse cambiarEstatus(@PathVariable Long id, @Valid @RequestBody EstatusRequest request) {
        return asesorService.cambiarEstatus(id, request);
    }

    @GetMapping("/{id}/desempeno")
    @Operation(summary = "Desempeno del asesor en un rango de fechas: visitas, cotizaciones generadas y tasa de conversion")
    public DesempenoAsesorResponse desempeno(@PathVariable Long id,
                                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return asesorService.desempeno(id, desde, hasta);
    }
}
