package com.project.mentalhealth.interfaces.api.v1.checkin;

import com.project.mentalhealth.application.service.CheckInService;
import com.project.mentalhealth.interfaces.api.v1.checkin.dto.CheckInRequest;
import com.project.mentalhealth.interfaces.api.v1.checkin.dto.CheckInStatusResponse;
import com.project.mentalhealth.interfaces.api.v1.common.ApiResponse;
import com.project.mentalhealth.interfaces.api.v1.mood.dto.MoodEntryResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${app.api.base-path}/checkin")
public class CheckInController {

    private final CheckInService checkInService;

    public CheckInController(CheckInService checkInService) {
        this.checkInService = checkInService;
    }

    @GetMapping
    public ApiResponse<CheckInStatusResponse> status(Authentication authentication) {
        return ApiResponse.success(checkInService.status(authentication.getName()));
    }

    @PostMapping
    public ApiResponse<MoodEntryResponse> submit(Authentication authentication,
                                                 @Valid @RequestBody CheckInRequest request) {
        return ApiResponse.success(checkInService.submit(authentication.getName(), request),
                "Check-in saved");
    }
}
