package mx.fnconcretos.comercial.client;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import mx.fnconcretos.comercial.dto.response.UbicacionResueltaResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Distancia real por carretera entre dos puntos (obra y planta), via Google Maps Distance Matrix
 * API, y resolucion de un link de Google Maps (corto o largo) a coordenadas + direccion/ciudad/
 * colonia (ver resolverUbicacionDesdeLink). Nunca bloquea nada si la key falta o la llamada falla
 * (mismo criterio defensivo que WhatsAppClient/NotificacionClient).
 */
@Slf4j
@Component
public class GoogleMapsClient {

    // Mismos 3 formatos que utils/maps.js (frontend) -- si el link YA trae coordenadas en el
    // texto, se parsean directo sin gastar ninguna llamada a Google.
    private static final Pattern PATRON_ARROBA = Pattern.compile("@(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)");
    private static final Pattern PATRON_QUERY = Pattern.compile("[?&]q=(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)");
    private static final Pattern PATRON_CRUDO = Pattern.compile("^\\s*(-?\\d+\\.\\d+)\\s*,\\s*(-?\\d+\\.\\d+)\\s*$");

    private final RestClient restClient;
    private final HttpClient httpClientRedirects;
    private final String apiKey;

    public GoogleMapsClient(@Value("${google-maps.api-key:}") String apiKey) {
        this.apiKey = apiKey;
        this.restClient = RestClient.builder()
                .baseUrl("https://maps.googleapis.com/maps/api")
                .build();
        this.httpClientRedirects = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .connectTimeout(Duration.ofSeconds(5))
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

    /**
     * Resuelve un link de Google Maps a coordenadas + direccion/ciudad/colonia. Si el link YA trae
     * coordenadas en el texto (formatos largos tipo @lat,lng o ?q=lat,lng), las usa directo sin
     * gastar ninguna llamada a Google. Si no (links cortos tipo maps.app.goo.gl/XXXX, que no traen
     * nada legible), sigue la redireccion para encontrar la URL real y sacarlas de ahi. En
     * cualquier caso, para direccion/ciudad/colonia SIEMPRE hace falta una llamada a Geocoding
     * (API que se factura) -- esa parte se omite solo si falta la key o si algo falla, nunca
     * bloquea el guardado del formulario (regresa null).
     */
    public UbicacionResueltaResponse resolverUbicacionDesdeLink(String linkCrudo) {
        if (linkCrudo == null || linkCrudo.isBlank()) return null;
        String link = linkCrudo.trim();

        BigDecimal[] coords = extraerCoordenadas(link);
        boolean vinoDeLinkLargo = coords != null;
        if (coords == null) {
            String resuelto = resolverRedireccion(link);
            if (resuelto != null) coords = extraerCoordenadas(resuelto);
        }
        if (coords == null) {
            log.warn("No se pudieron obtener coordenadas del link de Google Maps dado (ni directo ni tras seguir la redireccion)");
            return null;
        }

        UbicacionResueltaResponse.UbicacionResueltaResponseBuilder builder = UbicacionResueltaResponse.builder()
                .lat(coords[0])
                .lng(coords[1])
                .usoApiGoogle(!vinoDeLinkLargo);

        GeocodeInfo geocode = geocodificarInversa(coords[0], coords[1]);
        if (geocode != null) {
            builder.direccion(geocode.direccion).ciudad(geocode.ciudad).colonia(geocode.colonia).usoApiGoogle(true);
        }
        return builder.build();
    }

    /** null si el link ya no trae coordenadas en el texto (ninguno de los 3 formatos conocidos). */
    private BigDecimal[] extraerCoordenadas(String texto) {
        for (Pattern patron : List.of(PATRON_ARROBA, PATRON_QUERY, PATRON_CRUDO)) {
            Matcher m = patron.matcher(texto);
            if (m.find()) {
                try {
                    return new BigDecimal[]{new BigDecimal(m.group(1)), new BigDecimal(m.group(2))};
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return null;
    }

    /** Sigue la(s) redireccion(es) de un link corto (ej. maps.app.goo.gl/XXXX) y regresa la URL
     * final real -- null si la solicitud falla. No usa Google API, es solo HTTP. */
    private String resolverRedireccion(String link) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(link))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<Void> response = httpClientRedirects.send(request, HttpResponse.BodyHandlers.discarding());
            return response.uri().toString();
        } catch (Exception e) {
            log.warn("No se pudo seguir la redireccion del link de Google Maps '{}': {}", link, e.getMessage());
            return null;
        }
    }

    private GeocodeInfo geocodificarInversa(BigDecimal lat, BigDecimal lng) {
        if (apiKey == null || apiKey.isBlank()) return null;
        try {
            GeocodeResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/geocode/json")
                            .queryParam("latlng", lat + "," + lng)
                            .queryParam("language", "es")
                            .queryParam("key", apiKey)
                            .build())
                    .retrieve()
                    .body(GeocodeResponse.class);

            if (response == null || !"OK".equals(response.getStatus()) || response.getResults() == null || response.getResults().isEmpty()) {
                log.warn("Google Geocoding (reverso) respondio status={}", response != null ? response.getStatus() : null);
                return null;
            }

            GeocodeResult resultado = response.getResults().get(0);
            String calleNumero = null;
            String ciudad = null;
            String colonia = null;
            for (AddressComponent comp : resultado.getAddressComponents()) {
                if (comp.getTypes().contains("route")) {
                    calleNumero = comp.getLongName();
                } else if (comp.getTypes().contains("street_number") && calleNumero != null) {
                    calleNumero = calleNumero + " " + comp.getLongName();
                } else if (comp.getTypes().contains("locality")) {
                    ciudad = comp.getLongName();
                } else if (colonia == null && (comp.getTypes().contains("sublocality") || comp.getTypes().contains("neighborhood"))) {
                    colonia = comp.getLongName();
                }
            }

            GeocodeInfo info = new GeocodeInfo();
            info.direccion = calleNumero != null ? calleNumero : resultado.getFormattedAddress();
            info.ciudad = ciudad;
            info.colonia = colonia;
            return info;
        } catch (Exception e) {
            log.warn("No se pudo geocodificar en reversa ({},{}): {}", lat, lng, e.getMessage());
            return null;
        }
    }

    private static class GeocodeInfo {
        String direccion;
        String ciudad;
        String colonia;
    }

    @Data
    public static class GeocodeResponse {
        private String status;
        private List<GeocodeResult> results;
    }

    @Data
    public static class GeocodeResult {
        private String formattedAddress;
        private List<AddressComponent> addressComponents;
    }

    @Data
    public static class AddressComponent {
        private String longName;
        private List<String> types;
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
