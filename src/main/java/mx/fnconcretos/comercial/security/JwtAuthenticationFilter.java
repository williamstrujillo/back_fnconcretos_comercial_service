package mx.fnconcretos.comercial.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtValidator jwtValidator;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader(HEADER);
        if (header == null || !header.startsWith(PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(PREFIX.length());
        try {
            Claims claims = jwtValidator.parseAndValidate(token);
            if (!jwtValidator.isAccessToken(claims)) {
                filterChain.doFilter(request, response);
                return;
            }

            String rol = jwtValidator.extractRol(claims);
            List<String> permisos = jwtValidator.extractPermisos(claims);

            Stream<GrantedAuthority> rolAuthority = rol != null
                    ? Stream.of(new SimpleGrantedAuthority("ROLE_" + rol))
                    : Stream.empty();
            Stream<GrantedAuthority> permisoAuthorities = permisos != null
                    ? permisos.stream().map(SimpleGrantedAuthority::new)
                    : Stream.empty();
            List<GrantedAuthority> authorities = Stream.concat(rolAuthority, permisoAuthorities).toList();

            JwtPrincipal principal = new JwtPrincipal(
                    Long.valueOf(claims.getSubject()),
                    jwtValidator.extractUser(claims),
                    jwtValidator.extractIdEmpleado(claims),
                    rol,
                    permisos
            );

            var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Token invalido o expirado: {}", ex.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
