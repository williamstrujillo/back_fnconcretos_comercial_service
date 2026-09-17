package mx.fnconcretos.comercial.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import mx.fnconcretos.comercial.exception.EstadoInvalidoException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cliente hacia la API de WhatsApp Business de Meta (Cloud API) para notificar a clientes finales
 * (ej. "tu cotizacion ya esta lista"). A diferencia de ComercialClient/CatalogoClient/NotificacionClient
 * (llamadas entre microservicios propios con el JWT del usuario), este es un servicio externo de
 * terceros con su propio token (WHATSAPP_ACCESS_TOKEN) — nunca el JWT interno.
 *
 * Un mensaje de negocio hacia un numero que no le ha escrito a la cuenta en las ultimas 24h SOLO
 * puede enviarse como "template" (plantilla) previamente aprobada por Meta — no se puede mandar
 * texto libre. Ver plantilla "cotizacion_lista" (es_MX, categoria UTILITY).
 */
@Slf4j
@Component
public class WhatsAppClient {

    private final RestClient restClient;
    private final String phoneNumberId;
    private final String accessToken;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WhatsAppClient(@Value("${whatsapp.base-url}") String baseUrl,
                           @Value("${whatsapp.phone-number-id}") String phoneNumberId,
                           @Value("${whatsapp.access-token}") String accessToken) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.phoneNumberId = phoneNumberId;
        this.accessToken = accessToken;
    }

    /**
     * Envia un mensaje de plantilla aprobada. `parametros` llena, en orden, las variables {{1}},
     * {{2}}... del cuerpo de la plantilla (todas de tipo texto).
     */
    public void enviarPlantilla(String telefonoDestino, String nombrePlantilla, String idiomaCodigo, List<String> parametros) {
        enviarPlantilla(telefonoDestino, nombrePlantilla, idiomaCodigo, parametros, null);
    }

    /**
     * Igual que {@link #enviarPlantilla(String, String, String, List)} pero para plantillas que
     * ademas tienen un boton tipo URL con sufijo dinamico (ej. "https://fnconcretos.app/rastreo/{{1}}").
     * `parametroBoton` llena esa variable del boton (index 0, unico boton de la plantilla).
     */
    public void enviarPlantilla(String telefonoDestino, String nombrePlantilla, String idiomaCodigo,
                                 List<String> parametros, String parametroBoton) {
        if (phoneNumberId == null || phoneNumberId.isBlank() || accessToken == null || accessToken.isBlank()) {
            throw new IllegalStateException("WhatsApp no esta configurado (WHATSAPP_PHONE_NUMBER_ID/WHATSAPP_ACCESS_TOKEN)");
        }

        List<Map<String, Object>> componentes = new ArrayList<>();
        if (parametros != null && !parametros.isEmpty()) {
            List<Map<String, Object>> valores = new ArrayList<>();
            for (String p : parametros) {
                valores.add(Map.of("type", "text", "text", p == null ? "" : p));
            }
            componentes.add(Map.of("type", "body", "parameters", valores));
        }
        if (parametroBoton != null && !parametroBoton.isBlank()) {
            Map<String, Object> boton = new HashMap<>();
            boton.put("type", "button");
            boton.put("sub_type", "url");
            boton.put("index", "0");
            boton.put("parameters", List.of(Map.of("type", "text", "text", parametroBoton)));
            componentes.add(boton);
        }

        Map<String, Object> template = new HashMap<>();
        template.put("name", nombrePlantilla);
        template.put("language", Map.of("code", idiomaCodigo));
        if (!componentes.isEmpty()) {
            template.put("components", componentes);
        }

        Map<String, Object> body = new HashMap<>();
        body.put("messaging_product", "whatsapp");
        body.put("to", normalizarTelefonoMx(telefonoDestino));
        body.put("type", "template");
        body.put("template", template);

        try {
            restClient.post()
                    .uri("/{phoneNumberId}/messages", phoneNumberId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            String mensaje = extraerMensajeMeta(e.getResponseBodyAsString());
            log.warn("WhatsApp rechazo el envio a {} (plantilla {}): {}", telefonoDestino, nombrePlantilla, mensaje);
            throw new EstadoInvalidoException("No se pudo enviar el WhatsApp: " + mensaje);
        }
    }

    /** Los errores de Meta vienen anidados como {"error":{"message":"...","error_data":{"details":"..."}}} —
     * "details" suele traer la explicacion mas clara para el usuario final. */
    private String extraerMensajeMeta(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode error = root.path("error");
            String details = error.path("error_data").path("details").asText(null);
            if (details != null && !details.isBlank()) return details;
            String message = error.path("message").asText(null);
            return message != null ? message : body;
        } catch (Exception parseError) {
            return body;
        }
    }

    /**
     * Deja solo digitos y, si quedan 10 (numero mexicano capturado sin lada de pais), antepone 52 —
     * confirmado en vivo (2026-09-15) que la API acepta este formato sin el "1" extra que WhatsApp
     * historicamente pedia para moviles de Mexico.
     */
    private String normalizarTelefonoMx(String telefono) {
        String digitos = telefono.replaceAll("\\D", "");
        if (digitos.length() == 10) {
            return "52" + digitos;
        }
        return digitos;
    }
}
