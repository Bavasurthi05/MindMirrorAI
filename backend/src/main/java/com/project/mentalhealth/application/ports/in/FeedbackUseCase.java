package com.project.mentalhealth.application.ports.in;

import com.project.mentalhealth.interfaces.api.v1.feedback.dto.FeedbackRequest;
import com.project.mentalhealth.interfaces.api.v1.feedback.dto.FeedbackResponse;

import java.util.List;

public interface FeedbackUseCase {
    FeedbackResponse submit(String userEmail, FeedbackRequest request);
    List<FeedbackResponse> listAll();
}
