package mx.fnconcretos.comercial.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {

    /** Debe ser EXACTAMENTE el mismo secreto configurado en auth-service. */
    private String secret;
}
