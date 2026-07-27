package com.project.mentalhealth.application.ports.in;

import com.project.mentalhealth.interfaces.api.v1.trigger.dto.TriggerAnalyticsResponse;
import com.project.mentalhealth.interfaces.api.v1.trigger.dto.TriggerEntryRequest;
import com.project.mentalhealth.interfaces.api.v1.trigger.dto.TriggerEntryResponse;

import java.util.List;

public interface TriggerUseCase {
    TriggerEntryResponse log(String userEmail, TriggerEntryRequest request);
    List<TriggerEntryResponse> list(String userEmail);
    TriggerAnalyticsResponse analytics(String userEmail);
}
