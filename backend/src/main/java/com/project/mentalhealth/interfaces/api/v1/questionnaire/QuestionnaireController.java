package com.project.mentalhealth.interfaces.api.v1.questionnaire;

import com.project.mentalhealth.application.ports.in.QuestionnaireUseCase;
import com.project.mentalhealth.interfaces.api.v1.common.ApiResponse;
import com.project.mentalhealth.interfaces.api.v1.questionnaire.dto.QuestionnaireResultResponse;
import com.project.mentalhealth.interfaces.api.v1.questionnaire.dto.QuestionnaireSubmitRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${app.api.base-path}/questionnaire")
public class QuestionnaireController {

    private final QuestionnaireUseCase questionnaireUseCase;

    public QuestionnaireController(QuestionnaireUseCase questionnaireUseCase) {
        this.questionnaireUseCase = questionnaireUseCase;
    }

    @PostMapping
    public ApiResponse<QuestionnaireResultResponse> submit(Authentication authentication,
                                                           @Valid @RequestBody QuestionnaireSubmitRequest request) {
        return ApiResponse.success(questionnaireUseCase.submit(authentication.getName(), request), "Assessment recorded");
    }

    @GetMapping("/history")
    public ApiResponse<List<QuestionnaireResultResponse>> history(Authentication authentication) {
        return ApiResponse.success(questionnaireUseCase.history(authentication.getName()));
    }
}
