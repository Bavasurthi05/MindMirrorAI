package com.project.mentalhealth.application.ports.in;

import com.project.mentalhealth.interfaces.api.v1.trigger.dto.TriggerAnalyticsResponse;
import com.project.mentalhealth.interfaces.api.v1.trigger.dto.TriggerEntryRequest;
import com.project.mentalhealth.interfaces.api.v1.trigger.dto.TriggerEntryResponse;

import java.util.List;

public interface TriggerUseCase {

    TriggerEntryResponse log(String userEmail, TriggerEntryRequest request);

    List<TriggerEntryResponse> list(String userEmail);

    /** Auto-detected triggers awaiting the user's verdict. */
    List<TriggerEntryResponse> pendingConfirmation(String userEmail);

    /** Accept a detected trigger, optionally correcting its intensity. */
    TriggerEntryResponse confirm(String userEmail, Long id, Integer intensity);

    /** Reject a detected trigger: excluded from analytics, retained as negative signal. */
    TriggerEntryResponse dismiss(String userEmail, Long id);

    TriggerAnalyticsResponse analytics(String userEmail);
}
