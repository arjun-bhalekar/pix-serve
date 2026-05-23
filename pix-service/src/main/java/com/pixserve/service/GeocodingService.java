package com.pixserve.service;

import com.pixserve.model.NominatimResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class GeocodingService {

    private final RestTemplate restTemplate = new RestTemplate();

    private final static Logger LOGGER = LoggerFactory.getLogger(GeocodingService.class);

    public NominatimResponse getLocationData(String lat, String lon) {

        LOGGER.info("calling ex API for location info with lat : {}, lon : {}", lat, lon);

        String baseUrl = "https://nominatim.openstreetmap.org/reverse";

        // Build the URL with query string parameters safely encoded
        String finalUrl = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .queryParam("lat", lat)
                .queryParam("lon", lon)
                .queryParam("format", "json")
                .encode()
                .toUriString();

        // Attach required OpenStreetMap User-Agent policy headers
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "MySpringPixServeApp/1.0 (arjunbhalekar1708@gmail.com)");
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        // Fetch and automatically deserialize response into the Model object
        ResponseEntity<NominatimResponse> responseEntity = restTemplate.exchange(
                finalUrl,
                HttpMethod.GET,
                requestEntity,
                NominatimResponse.class
        );

        return responseEntity.getBody();
    }
}
