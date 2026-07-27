package com.project.mentalhealth.interfaces.api.v1.goal;

import com.project.mentalhealth.application.ports.in.GoalUseCase;
import com.project.mentalhealth.interfaces.api.v1.common.ApiResponse;
import com.project.mentalhealth.interfaces.api.v1.goal.dto.GoalRequest;
import com.project.mentalhealth.interfaces.api.v1.goal.dto.GoalResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("${app.api.base-path}/goals")
public class GoalController {

    private final GoalUseCase goalUseCase;

    public GoalController(GoalUseCase goalUseCase) {
        this.goalUseCase = goalUseCase;
    }

    @GetMapping
    public ApiResponse<List<GoalResponse>> list(Authentication authentication) {
        return ApiResponse.success(goalUseCase.list(authentication.getName()));
    }

    @PostMapping
    public ApiResponse<GoalResponse> create(Authentication authentication,
                                            @Valid @RequestBody GoalRequest request) {
        return ApiResponse.success(goalUseCase.create(authentication.getName(), request));
    }

    @PostMapping("/{id}/progress")
    public ApiResponse<GoalResponse> increment(Authentication authentication, @PathVariable Long id) {
        return ApiResponse.success(goalUseCase.incrementProgress(authentication.getName(), id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(Authentication authentication, @PathVariable Long id) {
        goalUseCase.delete(authentication.getName(), id);
        return ApiResponse.success(null);
    }
}
