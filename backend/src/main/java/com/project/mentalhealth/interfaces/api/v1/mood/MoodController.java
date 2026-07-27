package com.project.mentalhealth.interfaces.api.v1.mood;

import com.project.mentalhealth.application.ports.in.MoodUseCase;
import com.project.mentalhealth.interfaces.api.v1.common.ApiResponse;
import com.project.mentalhealth.interfaces.api.v1.mood.dto.MoodEntryRequest;
import com.project.mentalhealth.interfaces.api.v1.mood.dto.MoodEntryResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${app.api.base-path}/mood")
public class MoodController {

    private final MoodUseCase moodUseCase;

    public MoodController(MoodUseCase moodUseCase) {
        this.moodUseCase = moodUseCase;
    }

    @PostMapping
    public ApiResponse<MoodEntryResponse> log(Authentication authentication,
                                              @Valid @RequestBody MoodEntryRequest request) {
        return ApiResponse.success(moodUseCase.log(authentication.getName(), request), "Mood logged");
    }

    @GetMapping
    public ApiResponse<List<MoodEntryResponse>> recent(Authentication authentication,
                                                       @RequestParam(defaultValue = "30") int days) {
        return ApiResponse.success(moodUseCase.recent(authentication.getName(), days));
    }
}
