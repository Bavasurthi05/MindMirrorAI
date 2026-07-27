package com.project.mentalhealth.interfaces.api.v1.feedback.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public class FeedbackRequest {

    @Min(1)
    @Max(5)
    private int rating;

    @Size(max = 2000)
    private String message;

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
