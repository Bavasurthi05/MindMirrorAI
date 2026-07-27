package com.project.mentalhealth.application.ports.in;

import com.project.mentalhealth.interfaces.api.v1.questionnaire.dto.QuestionnaireResultResponse;
import com.project.mentalhealth.interfaces.api.v1.questionnaire.dto.QuestionnaireSubmitRequest;

import java.util.List;

public interface QuestionnaireUseCase {
    QuestionnaireResultResponse submit(String userEmail, QuestionnaireSubmitRequest request);
    List<QuestionnaireResultResponse> history(String userEmail);
}
