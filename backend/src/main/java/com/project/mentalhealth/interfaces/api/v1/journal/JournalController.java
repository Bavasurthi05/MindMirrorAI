package com.project.mentalhealth.interfaces.api.v1.journal;

import com.project.mentalhealth.application.ports.in.JournalUseCase;
import com.project.mentalhealth.interfaces.api.v1.common.ApiResponse;
import com.project.mentalhealth.interfaces.api.v1.common.PageResponse;
import com.project.mentalhealth.interfaces.api.v1.journal.dto.JournalEntryRequest;
import com.project.mentalhealth.interfaces.api.v1.journal.dto.JournalEntryResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${app.api.base-path}/journal")
public class JournalController {

    private final JournalUseCase journalUseCase;

    public JournalController(JournalUseCase journalUseCase) {
        this.journalUseCase = journalUseCase;
    }

    @PostMapping
    public ApiResponse<JournalEntryResponse> create(Authentication authentication,
                                                    @Valid @RequestBody JournalEntryRequest request) {
        return ApiResponse.success(journalUseCase.create(authentication.getName(), request), "Entry created");
    }

    @GetMapping
    public ApiResponse<PageResponse<JournalEntryResponse>> list(Authentication authentication,
                                                                @RequestParam(defaultValue = "0") int page,
                                                                @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(journalUseCase.list(authentication.getName(), page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<JournalEntryResponse> get(Authentication authentication, @PathVariable Long id) {
        return ApiResponse.success(journalUseCase.get(authentication.getName(), id));
    }

    @PutMapping("/{id}")
    public ApiResponse<JournalEntryResponse> update(Authentication authentication,
                                                    @PathVariable Long id,
                                                    @Valid @RequestBody JournalEntryRequest request) {
        return ApiResponse.success(journalUseCase.update(authentication.getName(), id, request), "Entry updated");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(Authentication authentication, @PathVariable Long id) {
        journalUseCase.delete(authentication.getName(), id);
        return ApiResponse.success(null, "Entry deleted");
    }
}
