package com.ecommerce.project.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Service
public class PixelServiceImpl implements PixelService {

    @Value("${pexel.api.key}")
    private String apiKey;

    private static final String BASE_URL = "https://api.pexels.com/v1/search";

    @Override
    public String fetchImage(String query) {
        RestTemplate restTemplate = new RestTemplate();
        String url = UriComponentsBuilder.fromHttpUrl(BASE_URL)
                .queryParam("query", query)
                .queryParam("per_page", 1)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", apiKey);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
        List<Map<String, Object>> photos = (List<Map<String, Object>>) response.getBody().get("photos");

        if (photos != null && !photos.isEmpty()) {
            Map<String, Object> firstPhoto = photos.get(0); // Get the first photo
            Map<String, String> src = (Map<String, String>) firstPhoto.get("src"); // Cast src to Map
            return src.get("original"); // Get the original image URL
        }

        return null; // No image found
    }
}
