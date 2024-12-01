package com.example.CalanderCanvas;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

@Service // Marks this class as a Spring service for dependency injection.
public class CanvasAPIService {

    // Injects the API key from the application properties or environment variables.
    @Value("${api.key}")
    private String CanvasAPIkey;

    // Injects the Canvas API base URL from the application properties or environment variables.
    @Value("${canvas.url}")
    private String canvasApiURL;

    // RestTemplate is used to send HTTP requests.
    private final RestTemplate restTemplate;

    // ObjectMapper is used to parse JSON responses into Java objects.
    private final ObjectMapper objectMapper;

    // Constructor to initialize RestTemplate and ObjectMapper via dependency injection.
    public CanvasAPIService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Fetches a list of courses the user is enrolled in as a student.
     *
     * @return A JsonNode object containing the course data.
     */
    public JsonNode getCourses() {
        // Builds the URL for fetching courses using the Canvas API.
        String url = UriComponentsBuilder.fromHttpUrl(canvasApiURL + "/api/v1/courses?enrollment_type=student")
            .queryParam("access_token", CanvasAPIkey) // Adds the API key as a query parameter.
            .toUriString();

        // Sends a GET request to the Canvas API to retrieve the list of courses.
        String response = restTemplate.getForObject(url, String.class);

        try {
            // Parses the JSON response into a JsonNode object.
            return objectMapper.readTree(response);
        } catch (Exception e) {
            // Throws a runtime exception if parsing fails.
            throw new RuntimeException("Failed to parse courses JSON response", e);
        }
    }

    /**
     * Retrieves grades for all assignments in all courses.
     *
     * @return A list of strings describing grades for each assignment.
     */
    public List<String> getGrades() {
        // Fetches the list of courses using the getCourses method.
        JsonNode courses = getCourses();
        List<String> grades = new ArrayList<>(); // Stores grade details for each assignment.

        // Ensures that the response is an array before iterating.
        if (courses.isArray()) {
            for (JsonNode course : courses) {
                // Extracts the course ID from the JSON response.
                String courseId = course.path("id").asText();

                // Builds the URL to fetch assignments for the current course.
                String assignmentsUrl = UriComponentsBuilder.fromHttpUrl(canvasApiURL + "/api/v1/courses/" + courseId + "/assignments")
                    .queryParam("access_token", CanvasAPIkey)
                    .toUriString();

                try {
                    // Sends a GET request to fetch assignments for the course.
                    String response = restTemplate.getForObject(assignmentsUrl, String.class);

                    // Parses the JSON response for assignments.
                    JsonNode assignments = objectMapper.readTree(response);

                    // Iterates over each assignment in the assignments array.
                    if (assignments.isArray()) {
                        for (JsonNode assignment : assignments) {
                            // Extracts assignment details.
                            String name = assignment.path("name").asText(); // Assignment name.
                            String pointsPossible = assignment.path("points_possible").asText(); // Maximum points.

                            // Builds the URL to fetch the user's submission for the assignment.
                            String assignmentId = assignment.path("id").asText();
                            String submissionUrl = UriComponentsBuilder.fromHttpUrl(canvasApiURL + "/api/v1/courses/" + courseId + "/assignments/" + assignmentId + "/submissions/self")
                                .queryParam("access_token", CanvasAPIkey)
                                .toUriString();

                            try {
                                // Sends a GET request to fetch the user's submission.
                                String submissionResponse = restTemplate.getForObject(submissionUrl, String.class);

                                // Parses the submission JSON to extract the score.
                                JsonNode submission = objectMapper.readTree(submissionResponse);
                                String pointsEarned = submission.path("score").asText("Not Available");

                                // Adds the grade details to the list.
                                grades.add("Assignment: " + name + ", Points Earned: " + pointsEarned + ", Total Points: " + pointsPossible);
                            } catch (Exception e) {
                                // Handles cases where the submission data is unavailable or invalid.
                                grades.add("Assignment: " + name + ", Points Earned: Not Available, Total Points: " + pointsPossible);
                            }
                        }
                    }
                } catch (Exception e) {
                    // Logs an error if fetching assignments fails for a course.
                    System.err.println("Failed to fetch assignments for course ID: " + courseId);
                }
            }
        }
        return grades; // Returns the list of grades.
    }

    /**
     * Retrieves all active calendar events across all courses.
     * An active event is one where blackout_date is set to false.
     *
     * @return A list of strings describing active calendar events.
     */
    public List<String> getActiveCalendarEvents() {
        // Builds the URL to fetch all calendar events from the Canvas API.
        String url = UriComponentsBuilder.fromHttpUrl(canvasApiURL + "/api/v1/calendar_events")
                .queryParam("access_token", CanvasAPIkey) // Adds the API key as a query parameter.
                .toUriString();

        try {
            // Sends a GET request to retrieve calendar events.
            String response = restTemplate.getForObject(url, String.class);

            // Parses the JSON response into a JsonNode.
            JsonNode rootNode = objectMapper.readTree(response);

            // List to store details of active events.
            List<String> activeEvents = new ArrayList<>();

            // Iterates over each calendar event in the response array.
            if (rootNode.isArray()) {
                for (JsonNode eventNode : rootNode) {
                    // Checks if the event is not a blackout date.
                    boolean blackoutDate = eventNode.path("blackout_date").asBoolean(false);
                    if (!blackoutDate) {
                        // Extracts event details.
                        String title = eventNode.path("title").asText("No Title"); // Event title.
                        String description = eventNode.path("description").asText("No Description"); // Event description.

                        // Adds the event details to the list.
                        activeEvents.add("Event: " + title + " | Description: " + description);
                    }
                }
            }
            return activeEvents; // Returns the list of active events.
        } catch (Exception e) {
            // Throws a runtime exception if an error occurs while fetching or parsing events.
            throw new RuntimeException("Error fetching active calendar events", e);
        }
    }
}
