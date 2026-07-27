package com.project.mentalhealth.application.ports.in;

import com.project.mentalhealth.interfaces.api.v1.mood.dto.MoodEntryRequest;
import com.project.mentalhealth.interfaces.api.v1.mood.dto.MoodEntryResponse;

import java.util.List;

public interface MoodUseCase {
    MoodEntryResponse log(String userEmail, MoodEntryRequest request);
    List<MoodEntryResponse> recent(String userEmail, int days);
}
