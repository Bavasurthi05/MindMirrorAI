package com.project.mentalhealth.interfaces.api.v1.feedback;

import com.project.mentalhealth.application.ports.in.FeedbackUseCase;
import com.project.mentalhealth.interfaces.api.v1.common.ApiResponse;
import com.project.mentalhealth.interfaces.api.v1.feedback.dto.FeedbackRequest;
import com.project.mentalhealth.interfaces.api.v1.feedback.dto.FeedbackResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${app.api.base-path}/feedback")
public class FeedbackController {

    private final FeedbackUseCase feedbackUseCase;

    public FeedbackController(FeedbackUseCase feedbackUseCase) {
        this.feedbackUseCase = feedbackUseCase;
    }

    @PostMapping
    public ApiResponse<FeedbackResponse> submit(Authentication authentication,
                                                @Valid @RequestBody FeedbackRequest request) {
        return ApiResponse.success(feedbackUseCase.submit(authentication.getName(), request));
    }
}
