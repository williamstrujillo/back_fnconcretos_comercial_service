package mx.fnconcretos.comercial.client;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Distancia real por carretera entre dos puntos (obra y planta), via Google Maps Distance Matrix
 * API. Es el unico uso de un API externo de mapas en el proyecto (el resto -- Obra.googleMapsLink,
 * Planta.googleMapsLink -- solo parsea coordenadas de un link pegado a mano, sin llamar a ningun
 * API). Se usa desde CotizacionService al guardar/actualizar una cotizacion, cuando obra y planta
 * ya tienen coordenadas conocidas -- nunca bloquea el guardado si la key falta, si faltan
 * coordenadas, o si la llamada falla (mismo criterio defensivo que WhatsAppClient/NotificacionClient).
 */
@Slf4j
@Component
public class GoogleMapsClient {

    private final RestClient restClient;
    private final String apiKey;

    public GoogleMapsClient(@Value("${google-maps.api-key:}") String apiKey) {
        this.apiKey = apiKey;
        this.restClient = RestClient.builder()
                .baseUrl("https://maps.googleapis.com/maps/api")
                .build();
    }

    /** Distancia por carretera en KM (1 decimal), o null si falta la key/coordenadas o si la
     * llamada no fue exitosa. */
    public BigDecimal calcularDistanciaKm(BigDecimal origenLat, BigDecimal origenLng, BigDecimal destinoLat, BigDecimal destinoLng) {
        if (apiKey == null || apiKey.isBlank()) return null;
        if (origenLat == null || origenLng == null || destinoLat == null || destinoLng == null) return null;

        try {
            DistanceMatrixResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/distancematrix/json")
                            .queryParam("origins", origenLat + "," + origenLng)
                            .queryParam("destinations", destinoLat + "," + destinoLng)
                            .queryParam("mode", "driving")
                            .queryParam("units", "metric")
                            .queryParam("key", apiKey)
                            .build())
                    .retrieve()
                    .body(DistanceMatrixResponse.class);

            if (response == null || !"OK".equals(response.getStatus())) {
                log.warn("Google Distance Matrix respondio status={}", response != null ? response.getStatus() : null);
                return null;
            }
            Element elemento = response.getRows().stream()
                    .findFirst()
                    .map(Row::getElements)
                    .filter(list -> !list.isEmpty())
                    .map(list -> list.get(0))
                    .orElse(null);
            if (elemento == null || !"OK".equals(elemento.getStatus()) || elemento.getDistance() == null) {
                log.warn("Google Distance Matrix sin ruta valida entre los puntos dados (elemento status={})",
                        elemento != null ? elemento.getStatus() : null);
                return null;
            }

            BigDecimal metros = BigDecimal.valueOf(elemento.getDistance().getValue());
            return metros.divide(BigDecimal.valueOf(1000), 1, RoundingMode.HALF_UP);
        } catch (Exception e) {
            log.warn("No se pudo calcular la distancia via Google Maps: {}", e.getMessage());
            return null;
        }
    }

    @Data
    public static class DistanceMatrixResponse {
        private String status;
        private List<Row> rows;
    }

    @Data
    public static class Row {
        private List<Element> elements;
    }

    @Data
    public static class Element {
        private String status;
        private Distance distance;
    }

    @Data
    public static class Distance {
        private long value;
    }
}
