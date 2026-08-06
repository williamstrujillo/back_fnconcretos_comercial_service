package mx.fnconcretos.comercial.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import mx.fnconcretos.comercial.config.JwtProperties;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.List;

@Component
public class JwtValidator {

    private final SecretKey signingKey;

    public JwtValidator(JwtProperties jwtProperties) {
        byte[] keyBytes = Base64.getDecoder().decode(jwtProperties.getSecret());
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public Claims parseAndValidate(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isAccessToken(Claims claims) {
        return "access".equals(claims.get("type", String.class));
    }

    @SuppressWarnings("unchecked")
    public List<String> extractPermisos(Claims claims) {
        return (List<String>) claims.get("permisos", List.class);
    }

    public String extractRol(Claims claims) {
        return claims.get("rol", String.class);
    }

    public String extractUser(Claims claims) {
        return claims.get("user", String.class);
    }

    public Long extractIdEmpleado(Claims claims) {
        Object value = claims.get("idEmpleado");
        return value != null ? Long.valueOf(value.toString()) : null;
    }
}
