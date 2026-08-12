package com.project.mentalhealth.interfaces.api.v1.journal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JournalEntryRequest {
    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must be at most 255 characters")
    private String title;

    @NotBlank(message = "Content is required")
    private String content;

    @Size(max = 50, message = "Mood must be at most 50 characters")
    private String mood;

    /** Which writing prompt the user answered, if any. */
    @Size(max = 64, message = "Prompt id must be at most 64 characters")
    private String promptId;
}
