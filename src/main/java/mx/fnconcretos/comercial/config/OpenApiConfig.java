package mx.fnconcretos.comercial.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI comercialServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("comercial-service - FN Concretos")
                        .description("El flujo de venta completo: clientes, obras compartidas, asesores, agenda, "
                                + "cotizaciones (con reciclaje y descuento especial) y su conversion a pedido con autorizaciones. "
                                + "Los tokens se emiten en auth-service; este servicio solo los valida.")
                        .version("1.0.0")
                        .contact(new Contact().name("FN Concretos - Sistemas")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .name(BEARER_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Access token obtenido en auth-service (/auth/login)")));
    }
}
