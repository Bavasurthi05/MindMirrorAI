package com.project.mentalhealth.application.ports.in;

import com.project.mentalhealth.interfaces.api.v1.common.PageResponse;
import com.project.mentalhealth.interfaces.api.v1.journal.dto.JournalEntryRequest;
import com.project.mentalhealth.interfaces.api.v1.journal.dto.JournalEntryResponse;

public interface JournalUseCase {
    JournalEntryResponse create(String userEmail, JournalEntryRequest request);
    PageResponse<JournalEntryResponse> list(String userEmail, int page, int size);
    JournalEntryResponse get(String userEmail, Long id);
    JournalEntryResponse update(String userEmail, Long id, JournalEntryRequest request);
    void delete(String userEmail, Long id);
}
