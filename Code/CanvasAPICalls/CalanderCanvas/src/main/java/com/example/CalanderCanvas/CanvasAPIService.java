package com.example.CalanderCanvas;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

@Service
public class CanvasAPIService {

    @Value("${api.key}")
    private String CanvasAPIkey;

    @Value("${canvas.url}")
    private String canvasApiURL;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public CanvasAPIService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    // Gets the list of courses
    public JsonNode getCourses() {
        String url = UriComponentsBuilder.fromHttpUrl(canvasApiURL + "/api/v1/courses?enrollment_type=student")
            .queryParam("access_token", CanvasAPIkey)
            .toUriString();
        String response = restTemplate.getForObject(url, String.class);
        try {
            return objectMapper.readTree(response);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse courses JSON response", e);
        }
    }

public List<String> getGrades() {
    JsonNode courses = getCourses();
    List<String> grades = new ArrayList<>();

    if (courses.isArray()) {
        for (JsonNode course : courses) {
            String courseId = course.path("id").asText();
            String assignmentsUrl = UriComponentsBuilder.fromHttpUrl(canvasApiURL + "/api/v1/courses/" + courseId + "/assignments")
                .queryParam("access_token", CanvasAPIkey)
                .toUriString();

            try {
                String response = restTemplate.getForObject(assignmentsUrl, String.class);
                JsonNode assignments = objectMapper.readTree(response);

                if (assignments.isArray()) {
                    for (JsonNode assignment : assignments) {
                        String name = assignment.path("name").asText();
                        String pointsPossible = assignment.path("points_possible").asText();

                        // Fetch submission data to get the score (final grade)
                        String assignmentId = assignment.path("id").asText();
                        String submissionUrl = UriComponentsBuilder.fromHttpUrl(canvasApiURL + "/api/v1/courses/" + courseId + "/assignments/" + assignmentId + "/submissions/self")
                            .queryParam("access_token", CanvasAPIkey)
                            .toUriString();

                        try {
                            String submissionResponse = restTemplate.getForObject(submissionUrl, String.class);
                            JsonNode submission = objectMapper.readTree(submissionResponse);
                            String pointsEarned = submission.path("score").asText(); // Points earned

                            grades.add("Assignment: " + name + ", Points Earned: " + pointsEarned + ", Total Points: " + pointsPossible);
                        } catch (Exception e) {
                            grades.add("Assignment: " + name + ", Points Earned: Not Available, Total Points: " + pointsPossible);
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to fetch assignments for course ID: " + courseId);
            }
        }
    }
    return grades;
}


}
