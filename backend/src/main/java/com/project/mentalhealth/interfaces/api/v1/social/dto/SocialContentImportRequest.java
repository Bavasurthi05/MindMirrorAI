package com.project.mentalhealth.interfaces.api.v1.social.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SocialContentImportRequest {
    @NotBlank
    private String provider;

    @NotBlank
    private String content;
}
