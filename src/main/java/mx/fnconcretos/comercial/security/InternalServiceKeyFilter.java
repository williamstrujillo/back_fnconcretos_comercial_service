package mx.fnconcretos.comercial.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Exige el header X-Internal-Service-Key en los endpoints de uso puramente
 * interno (los que solo invocan otros microservicios, nunca la UI) -- capa
 * adicional sobre el JWT ya reenviado, para que un JWT de usuario valido no
 * baste por si solo para llamar estos endpoints directamente.
 */
@Component
public class InternalServiceKeyFilter extends OncePerRequestFilter {

    private static final String PATRON_REGISTRAR_ENTREGA = "/pedidos/*/registrar-entrega";
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private final String claveEsperada;

    public InternalServiceKeyFilter(@Value("${internal.service-key}") String claveEsperada) {
        this.claveEsperada = claveEsperada;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (esRutaInterna(request) && !claveEsperada.equals(request.getHeader("X-Internal-Service-Key"))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Falta o es invalida la clave de servicio interno");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean esRutaInterna(HttpServletRequest request) {
        return "PATCH".equals(request.getMethod()) && pathMatcher.match(PATRON_REGISTRAR_ENTREGA, request.getServletPath());
    }
}
