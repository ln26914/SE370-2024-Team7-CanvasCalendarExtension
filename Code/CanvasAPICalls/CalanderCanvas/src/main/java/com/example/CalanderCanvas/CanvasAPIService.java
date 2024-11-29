package com.example.CalanderCanvas;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class CanvasAPIService {
    @Value("${api.key}")
    private String CanvasAPIkey;

    @Value("${canvas.url}")
    private String canvasApiURL;

    private final RestTemplate restTemplate;

    public CanvasAPIService(RestTemplate restTemplate){
        this.restTemplate = restTemplate;
    }

    //gets the courses
    public String getCourses(){
        String url = UriComponentsBuilder.fromHttpUrl(canvasApiURL+ "/api/v1/courses?enrollment_type=student")
            .queryParam("access_token", CanvasAPIkey)
            .toUriString();
        return restTemplate.getForObject(url, String.class);
    }


}
