package com.example.CalanderCanvas;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin
public class CanvasController {


    @GetMapping("/health-check")
    public String getHealthCheck(){
        return "Situation Normal";
    }

    private final CanvasAPIService canvasService;

    public CanvasController(CanvasAPIService canvasService) {
        this.canvasService = canvasService;
    }

    @GetMapping("/courses")
    public String getCourses() {
        return canvasService.getCourses();
    }



}
