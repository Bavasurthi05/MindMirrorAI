package com.project.mentalhealth.interfaces.api.v1.trigger;

import com.project.mentalhealth.application.ports.in.TriggerUseCase;
import com.project.mentalhealth.interfaces.api.v1.common.ApiResponse;
import com.project.mentalhealth.interfaces.api.v1.trigger.dto.TriggerAnalyticsResponse;
import com.project.mentalhealth.interfaces.api.v1.trigger.dto.TriggerEntryRequest;
import com.project.mentalhealth.interfaces.api.v1.trigger.dto.TriggerEntryResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${app.api.base-path}/triggers")
public class TriggerController {

    private final TriggerUseCase triggerUseCase;

    public TriggerController(TriggerUseCase triggerUseCase) {
        this.triggerUseCase = triggerUseCase;
    }

    @PostMapping
    public ApiResponse<TriggerEntryResponse> log(Authentication authentication,
                                                 @Valid @RequestBody TriggerEntryRequest request) {
        return ApiResponse.success(triggerUseCase.log(authentication.getName(), request), "Trigger logged");
    }

    @GetMapping
    public ApiResponse<List<TriggerEntryResponse>> list(Authentication authentication) {
        return ApiResponse.success(triggerUseCase.list(authentication.getName()));
    }

    @GetMapping("/analytics")
    public ApiResponse<TriggerAnalyticsResponse> analytics(Authentication authentication) {
        return ApiResponse.success(triggerUseCase.analytics(authentication.getName()));
    }
}
